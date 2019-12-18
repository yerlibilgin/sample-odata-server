package eu.toop;


import eu.toop.model.ToopDirClient;
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
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class DiscoveryODataWrapper implements EntityCollectionProcessor, EntityProcessor,
    PrimitiveProcessor, PrimitiveValueProcessor, ComplexProcessor, ErrorProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryODataWrapper.class);

  private static final DiscoveryODataWrapper instance = new DiscoveryODataWrapper();
  private OData odata;
  private ServiceMetadata serviceMetadata;

  public static DiscoveryODataWrapper getInstance() {
    return instance;
  }

  public DiscoveryODataWrapper() {

  }


  @Override
  public void init(OData odata, ServiceMetadata serviceMetadata) {
    this.odata = odata;
    this.serviceMetadata = serviceMetadata;
  }

  @Override
  public void readComplex(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    LOGGER.debug("readComplex " + uriInfo.toString());
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


  public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
      throws ODataApplicationException, SerializerException {
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
    if(expandOption != null) {
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
        EdmEntityType expandEdmEntityType = edmNavigationProperty.getType();
        String navPropName = edmNavigationProperty.getName();

        // build the inline data
        Link link = new Link();
        link.setTitle(navPropName);
        link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
        link.setRel(Constants.NS_ASSOCIATION_LINK_REL + navPropName);

        if(edmNavigationProperty.isCollection()){ // in case of Categories(1)/$expand=Products
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


  private boolean isContNav(UriInfo uriInfo) {
    List<UriResource> resourceParts = uriInfo.getUriResourceParts();
    for (UriResource resourcePart : resourceParts) {
      if (resourcePart instanceof UriResourceNavigation) {
        UriResourceNavigation navResource = (UriResourceNavigation) resourcePart;
        if (navResource.getProperty().containsTarget()) {
          return true;
        }
      }
    }
    return false;
  }


  private ContextURL getContextUrl(final EdmEntitySet entitySet, final boolean isSingleEntity,
                                   final ExpandOption expand, final SelectOption select, final String navOrPropertyPath)
      throws SerializerException {

    return ContextURL.with().entitySet(entitySet)
        .selectList(odata.createUriHelper().buildContextURLSelectList(entitySet.getEntityType(), expand, select))
        .suffix(isSingleEntity ? ContextURL.Suffix.ENTITY : null)
        .navOrPropertyPath(navOrPropertyPath)
        .build();
  }


  public static boolean isODataMetadataNone(final ContentType contentType) {
    return contentType.isCompatible(ContentType.APPLICATION_JSON)
        && ContentType.VALUE_ODATA_METADATA_NONE.equalsIgnoreCase(
        contentType.getParameter(ContentType.PARAMETER_ODATA_METADATA));
  }

  private EdmEntitySet getEdmEntitySet(final UriInfoResource uriInfo) throws ODataApplicationException {
    final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
    /*
     * To get the entity set we have to interpret all URI segments
     */
    if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
      throw new ODataApplicationException("Invalid resource type for first segment.",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    /*
     * Here we should interpret the whole URI but in this example we do not support navigation so we throw an exception
     */

    final UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);

    System.out.println("Get entity set: " + uriResource.getEntitySet());
    System.out.println("Get entity set: " + uriResource.getEntityType());
    System.out.println("Get entity set: " + uriResource.getKeyPredicates().stream().map(mapper -> mapper.getAlias()));
    return uriResource.getEntitySet();
  }

  @Override
  public void readPrimitiveValue(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    LOGGER.debug("readPrimitiveValue " + uriInfo.toString());

  }

  public void readPrimitive(ODataRequest request, ODataResponse response,
                            UriInfo uriInfo, ContentType responseFormat)
      throws ODataApplicationException, SerializerException {

    // 1. Retrieve info from URI
    // 1.1. retrieve the info about the requested entity set
    List<UriResource> resourceParts = uriInfo.getUriResourceParts();
    // Note: only in our example we can rely that the first segment is the EntitySet
    UriResourceEntitySet uriEntityset = (UriResourceEntitySet) resourceParts.get(0);
    EdmEntitySet edmEntitySet = uriEntityset.getEntitySet();
    // the key for the entity
    List<UriParameter> keyPredicates = uriEntityset.getKeyPredicates();

    // 1.2. retrieve the requested (Edm) property
    // the last segment is the Property
    UriResourceProperty uriProperty = (UriResourceProperty) resourceParts.get(resourceParts.size() - 1);
    EdmProperty edmProperty = uriProperty.getProperty();
    String edmPropertyName = edmProperty.getName();
    // in our example, we know we have only primitive types in our model
    EdmPrimitiveType edmPropertyType = (EdmPrimitiveType) edmProperty.getType();

    // 2. retrieve data from backend
    // 2.1. retrieve the entity data, for which the property has to be read
    Entity entity = ToopDirClient.readEntityData(edmEntitySet, keyPredicates);
    if (entity == null) { // Bad request
      throw new ODataApplicationException("Entity not found",
          HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
    }

    // 2.2. retrieve the property data from the entity
    Property property = entity.getProperty(edmPropertyName);
    if (property == null) {
      throw new ODataApplicationException("Property not found",
          HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
    }

    // 3. serialize
    Object value = property.getValue();
    if (value != null) {
      // 3.1. configure the serializer
      ODataSerializer serializer = odata.createSerializer(responseFormat);

      ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).navOrPropertyPath(edmPropertyName).build();
      PrimitiveSerializerOptions options = PrimitiveSerializerOptions.with().contextURL(contextUrl).build();
      // 3.2. serialize
      SerializerResult serializerResult = serializer.primitive(serviceMetadata, edmPropertyType, property, options);
      InputStream propertyStream = serializerResult.getContent();

      // 4. configure the response object
      response.setContent(propertyStream);
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
      response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    } else {
      // in case there's no value for the property, we can skip the serialization
      response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }
  }

  @Override
  public void updateComplex(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    throw new ODataNotSupportedException("Not allowed");
  }

  @Override
  public void deleteComplex(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {
    throw new ODataNotSupportedException("Not allowed");
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

  @Override
  public void updatePrimitiveValue(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    throw new ODataNotSupportedException("Not allowed");
  }

  @Override
  public void deletePrimitiveValue(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {
    throw new ODataNotSupportedException("Not allowed");
  }

  @Override
  public void updatePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
                              ContentType responseFormat) {
    throw new ODataNotSupportedException("Not allowed");
  }

  @Override
  public void deletePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo) {
    throw new ODataNotSupportedException("Not allowed");
  }

  @Override
  public void processError(ODataRequest request, ODataResponse response, ODataServerError serverError, ContentType responseFormat) {
    LOGGER.error(" server error " + serverError.getException().getMessage(), serverError.getException());
  }
}
