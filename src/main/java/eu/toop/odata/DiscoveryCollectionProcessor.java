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

public class DiscoveryCollectionProcessor implements EntityCollectionProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryCollectionProcessor.class);

  private OData odata;
  private ServiceMetadata serviceMetadata;


  @Override
  public void init(OData odata, ServiceMetadata serviceMetadata) {
    this.odata = odata;
    this.serviceMetadata = serviceMetadata;
  }

  public void readEntityCollection(ODataRequest request, ODataResponse response,
                                   UriInfo uriInfo, ContentType responseFormat)
      throws ODataApplicationException, SerializerException {

    // 1st retrieve the requested EdmEntitySet from the uriInfo
    List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    // in our example, the first segment is the EntitySet
    UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
    EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

    // 2nd: fetch the data from backend for this requested EntitySetName
    EntityCollection entityCollection = ToopDirClient.readEntitySetData(edmEntitySet);

    FilterOption filterOption = uriInfo.getFilterOption();
    if(filterOption != null) {
      Util.applyFilterOption(entityCollection, filterOption);
    }

    // 3rd: apply system query options
    SelectOption selectOption = uriInfo.getSelectOption();
    ExpandOption expandOption = uriInfo.getExpandOption();

    // handle $expand
    // in our example: http://localhost:8080/DemoService/DemoService.svc/Categories/$expand=Products
    // or http://localhost:8080/DemoService/DemoService.svc/Products?$expand=Category
    if (expandOption != null) {
      // retrieve the EdmNavigationProperty from the expand expression
      // Note: in our example, we have only one NavigationProperty, so we can directly access it
      EdmNavigationProperty edmNavigationProperty = null;
      ExpandItem expandItem = expandOption.getExpandItems().get(0);
      if(expandItem.isStar()) {
        List<EdmNavigationPropertyBinding> bindings = edmEntitySet.getNavigationPropertyBindings();
        // we know that there are navigation bindings
        // however normally in this case a check if navigation bindings exists is done
        if(!bindings.isEmpty()) {
          // can in our case only be 'Category' or 'Products', so we can take the first
          EdmNavigationPropertyBinding binding = bindings.get(0);
          EdmElement property = edmEntitySet.getEntityType().getProperty(binding.getPath());
          // we don't need to handle error cases, as it is done in the Olingo library
          if(property instanceof EdmNavigationProperty) {
            edmNavigationProperty = (EdmNavigationProperty) property;
          }
        }
      } else {
        // can be 'Category' or 'Products', no path supported
        UriResource uriResource = expandItem.getResourcePath().getUriResourceParts().get(0);
        // we don't need to handle error cases, as it is done in the Olingo library
        if(uriResource instanceof UriResourceNavigation) {
          edmNavigationProperty = ((UriResourceNavigation) uriResource).getProperty();
        }
      }

      // can be 'Category' or 'Products', no path supported
      // we don't need to handle error cases, as it is done in the Olingo library
      if(edmNavigationProperty != null) {
        String navPropName = edmNavigationProperty.getName();
        EdmEntityType expandEdmEntityType = edmNavigationProperty.getType();

        List<Entity> entityList = entityCollection.getEntities();
        for (Entity entity : entityList) {
          Link link = new Link();
          link.setTitle(navPropName);
          link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
          link.setRel(Constants.NS_ASSOCIATION_LINK_REL + navPropName);

          if (edmNavigationProperty.isCollection()) { // in case of Categories/$expand=Products
            // fetch the data for the $expand (to-many navigation) from backend
            EntityCollection expandEntityCollection = ToopDirClient.getRelatedEntityCollection(entity, expandEdmEntityType);

            if (expandItem.getFilterOption() != null){
              Util.applyFilterOption(expandEntityCollection, expandItem.getFilterOption());
            }

            link.setInlineEntitySet(expandEntityCollection);
            //link.setHref(expandEntityCollection.getId().toASCIIString());
          } else { // in case of Products?$expand=Category
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
    }
    // 4th: serialize
    EdmEntityType edmEntityType = edmEntitySet.getEntityType();
    // we need the property names of the $select, in order to build the context URL
    String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType, expandOption, selectOption);
    ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).selectList(selectList).build();

    // adding the selectOption to the serializerOpts will actually tell the lib to do the job
    final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
    EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
        .contextURL(contextUrl)
        .select(selectOption)
        .expand(expandOption)
        .id(id)
        .build();

    ODataSerializer serializer = odata.createSerializer(responseFormat);
    SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entityCollection, opts);

    // 5th: configure the response object: set the body, headers and status code
    response.setContent(serializerResult.getContent());
    response.setStatusCode(HttpStatusCode.OK.getStatusCode());

    response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
  }
}
