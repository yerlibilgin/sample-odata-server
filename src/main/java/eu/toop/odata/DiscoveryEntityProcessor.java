package eu.toop.odata;


import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.ex.ODataNotSupportedException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.*;
import org.apache.olingo.server.api.serializer.*;
import org.apache.olingo.server.api.uri.*;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class DiscoveryEntityProcessor implements EntityProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryEntityProcessor.class);

  private OData odata;
  private ServiceMetadata serviceMetadata;

  @Override
  public void init(OData odata, ServiceMetadata serviceMetadata) {
    this.odata = odata;
    this.serviceMetadata = serviceMetadata;
  }

  @Override
  public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
      throws SerializerException {
    // 1. retrieve the Entity Type
    List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    // Note: only in our example we can assume that the first segment is the EntitySet
    UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
    EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

    // 2. retrieve the data from backend
    List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
    Entity entity = ToopDirClient.readEntityData(edmEntitySet, keyPredicates);

    // 3. apply system query options

    // handle $select
    SelectOption selectOption = uriInfo.getSelectOption();
    // in our example, we don't have performance issues, so we can rely upon the handling in the Olingo lib
    // nothing else to be done

    // handle $expand
    ExpandOption expandOption = uriInfo.getExpandOption();
    // in our example: http://localhost:8080/DemoService/DemoService.svc/Categories(1)/$expand=Products
    // or http://localhost:8080/DemoService/DemoService.svc/Products(1)?$expand=Category
    if (expandOption != null) {
      // retrieve the EdmNavigationProperty from the expand expression
      // Note: in our example, we have only one NavigationProperty, so we can directly access it
      EdmNavigationProperty edmNavigationProperty = null;
      ExpandItem expandItem = expandOption.getExpandItems().get(0);
      if (expandItem.isStar()) {
        List<EdmNavigationPropertyBinding> bindings = edmEntitySet.getNavigationPropertyBindings();
        // we know that there are navigation bindings
        // however normally in this case a check if navigation bindings exists is done
        if (!bindings.isEmpty()) {
          // can in our case only be 'Category' or 'Products', so we can take the first
          EdmNavigationPropertyBinding binding = bindings.get(0);
          EdmElement property = edmEntitySet.getEntityType().getProperty(binding.getPath());
          // we don't need to handle error cases, as it is done in the Olingo library
          if (property instanceof EdmNavigationProperty) {
            edmNavigationProperty = (EdmNavigationProperty) property;
          }
        }
      } else {
        // can be 'Category' or 'Products', no path supported
        UriResource uriResource = expandItem.getResourcePath().getUriResourceParts().get(0);
        // we don't need to handle error cases, as it is done in the Olingo library
        if (uriResource instanceof UriResourceNavigation) {
          edmNavigationProperty = ((UriResourceNavigation) uriResource).getProperty();
        }
      }

      // can be 'Category' or 'Products', no path supported
      // we don't need to handle error cases, as it is done in the Olingo library
      if (edmNavigationProperty != null) {
        EdmEntityType expandEdmEntityType = edmNavigationProperty.getType();
        String navPropName = edmNavigationProperty.getName();

        // build the inline data
        Link link = new Link();
        link.setTitle(navPropName);
        link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
        link.setRel(Constants.NS_ASSOCIATION_LINK_REL + navPropName);

        if (edmNavigationProperty.isCollection()) { // in case of Categories(1)/$expand=Products
          // fetch the data for the $expand (to-many navigation) from backend
          // here we get the data for the expand
          EntityCollection expandEntityCollection = ToopDirClient.getRelatedEntityCollection(entity, expandEdmEntityType);
          link.setInlineEntitySet(expandEntityCollection);
          //link.setHref(expandEntityCollection.getId().toASCIIString());
        } else {  // in case of Products(1)?$expand=Category
          // fetch the data for the $expand (to-one navigation) from backend
          // here we get the data for the expand
          Entity expandEntity = ToopDirClient.getRelatedEntity(entity, expandEdmEntityType);
          link.setInlineEntity(expandEntity);
          link.setHref(expandEntity.getId().toASCIIString());
        }

        // set the link - containing the expanded data - to the current entity
        entity.getNavigationLinks().add(link);
      }
    }


    // 4. serialize
    EdmEntityType edmEntityType = edmEntitySet.getEntityType();
    // we need the property names of the $select, in order to build the context URL
    String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType, expandOption, selectOption);
    ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet)
        .selectList(selectList)
        .suffix(Suffix.ENTITY).build();

    // make sure that $expand and $select are considered by the serializer
    // adding the selectOption to the serializerOpts will actually tell the lib to do the job
    EntitySerializerOptions opts = EntitySerializerOptions.with()
        .contextURL(contextUrl)
        .select(selectOption)
        .expand(expandOption)
        .build();

    ODataSerializer serializer = this.odata.createSerializer(responseFormat);
    SerializerResult serializerResult = serializer.entity(serviceMetadata, edmEntityType, entity, opts);

    // 5. configure the response object
    response.setContent(serializerResult.getContent());
    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
  }

  @Override
  public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    throw new ODataNotSupportedException("Not allowed");
  }

  @Override
  public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    throw new ODataNotSupportedException("Not allowed");
  }

  @Override
  public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {
    throw new ODataNotSupportedException("Not allowed");
  }
}
