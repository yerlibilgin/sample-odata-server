package eu.toop;


import eu.toop.model.ToopDirClient;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.ex.ODataNotSupportedException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.*;
import org.apache.olingo.server.api.serializer.*;
import org.apache.olingo.server.api.uri.*;
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

  //@Override
  public void readEntdityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    UriInfoResource uriInfoResource = uriInfo.asUriInfoResource();
    LOGGER.debug("readEntityCollection " + uriInfoResource);

    EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfoResource);

    LOGGER.debug("EDM ENTITY SET: " + edmEntitySet.getName());

    EntityCollection entitySet = new EntityCollection();

    //Map<Integer, BusinessCardType> all = ToopDirClient.getAllParticipantIDs();
//
    //all.forEach((id, bt) -> {
    //  entitySet.getEntities().add(ODataDirectoryHelper.asEntity(id, bt));
    //});

    // Next we create a serializer based on the requested format. This could also be a custom format but we do not
    // support them in this example
    ODataSerializer serializer = odata.createSerializer(responseFormat);

    // Now the content is serialized using the serializer.
    final ExpandOption expand = uriInfo.getExpandOption();
    final SelectOption select = uriInfo.getSelectOption();
    final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
    InputStream serializedContent = serializer.entityCollection(serviceMetadata,
        edmEntitySet.getEntityType(),
        entitySet,
        EntityCollectionSerializerOptions.with()
            .id(id)
            .contextURL(isODataMetadataNone(responseFormat) ? null :
                getContextUrl(edmEntitySet, false, expand, select, null))
            .count(uriInfo.getCountOption())
            .expand(expand).select(select)
            .build()).getContent();

    // Finally we set the response data, headers and status code
    response.setContent(serializedContent);
    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
  }

  public void readEntityCollection(ODataRequest request, ODataResponse response,
                                   UriInfo uriInfo, ContentType responseFormat)
      throws ODataApplicationException, SerializerException {

    EdmEntitySet responseEdmEntitySet = null; // we'll need this to build the ContextURL
    EntityCollection responseEntityCollection = null; // we'll need this to set the response body
    EdmEntityType responseEdmEntityType = null;

    // 1st retrieve the requested EntitySet from the uriInfo (representation of the parsed URI)
    List<UriResource> resourceParts = uriInfo.getUriResourceParts();
    int segmentCount = resourceParts.size();

    UriResource uriResource = resourceParts.get(0); // in our example, the first segment is the EntitySet
    if (!(uriResource instanceof UriResourceEntitySet)) {
      throw new ODataApplicationException("Only EntitySet is supported",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
    EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();

    if (segmentCount == 1) { // this is the case for: DemoService/DemoService.svc/Categories
      responseEdmEntitySet = startEdmEntitySet; // the response body is built from the first (and only) entitySet

      // 2nd: fetch the data from backend for this requested EntitySetName and deliver as EntitySet
      responseEntityCollection = ToopDirClient.readEntitySetData(startEdmEntitySet);
    } else if (segmentCount == 2) { // in case of navigation: DemoService.svc/Categories(3)/Products

      UriResource lastSegment = resourceParts.get(1); // in our example we don't support more complex URIs
      if (lastSegment instanceof UriResourceNavigation) {
        UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
        EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
        EdmEntityType targetEntityType = edmNavigationProperty.getType();
        if (!edmNavigationProperty.containsTarget()) {
          // from Categories(1) to Products
          responseEdmEntitySet = Util.getNavigationTargetEntitySet(startEdmEntitySet, edmNavigationProperty);
        } else {
          responseEdmEntitySet = startEdmEntitySet;
          responseEdmEntityType = targetEntityType;
        }

        // 2nd: fetch the data from backend
        // first fetch the entity where the first segment of the URI points to
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        // e.g. for Categories(3)/Products we have to find the single entity: Category with ID 3
        Entity sourceEntity = ToopDirClient.readEntityData(startEdmEntitySet, keyPredicates);
        // error handling for e.g. DemoService.svc/Categories(99)/Products
        if (sourceEntity == null) {
          throw new ODataApplicationException("Entity not found.",
              HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
        }
        // then fetch the entity collection where the entity navigates to
        // note: we don't need to check uriResourceNavigation.isCollection(),
        // because we are the EntityCollectionProcessor
        responseEntityCollection = ToopDirClient.getRelatedEntityCollection(sourceEntity, targetEntityType);
      }
    } else { // this would be the case for e.g. Products(1)/Category/Products
      throw new ODataApplicationException("Not supported",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    ContextURL contextUrl = null;
    EdmEntityType edmEntityType = null;
    // 3rd: create and configure a serializer
    if (isContNav(uriInfo)) {
      contextUrl = ContextURL.with().entitySetOrSingletonOrType(request.getRawODataPath()).build();
      edmEntityType = responseEdmEntityType;
    } else {
      contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).build();
      edmEntityType = responseEdmEntitySet.getEntityType();
    }
    final String id = request.getRawBaseUri() + "/" + responseEdmEntitySet.getName();
    EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
        .contextURL(contextUrl).id(id).build();

    ODataSerializer serializer = odata.createSerializer(responseFormat);
    SerializerResult serializerResult = serializer.entityCollection(this.serviceMetadata, edmEntityType,
        responseEntityCollection, opts);

    // 4th: configure the response object: set the body, headers and status code
    response.setContent(serializerResult.getContent());
    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
    response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
  }


  public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
      throws ODataApplicationException, SerializerException {
    EdmEntityType responseEdmEntityType = null; // we'll need this to build the ContextURL
    Entity responseEntity = null; // required for serialization of the response body
    EdmEntitySet responseEdmEntitySet = null; // we need this for building the contextUrl

    // 1st step: retrieve the requested Entity: can be "normal" read operation, or navigation (to-one)
    List<UriResource> resourceParts = uriInfo.getUriResourceParts();
    int segmentCount = resourceParts.size();

    UriResource uriResource = resourceParts.get(0); // in our example, the first segment is the EntitySet
    if (!(uriResource instanceof UriResourceEntitySet)) {
      throw new ODataApplicationException("Only EntitySet is supported",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
    EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();

    // Analyze the URI segments
    if (segmentCount == 1) { // no navigation
      responseEdmEntityType = startEdmEntitySet.getEntityType();
      responseEdmEntitySet = startEdmEntitySet; // since we have only one segment

      // 2. step: retrieve the data from backend
      List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
      responseEntity = ToopDirClient.readEntityData(startEdmEntitySet, keyPredicates);
    } else if (segmentCount == 2) { // navigation
      UriResource navSegment = resourceParts.get(1); // in our example we don't support more complex URIs
      if (navSegment instanceof UriResourceNavigation) {
        UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) navSegment;
        EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
        responseEdmEntityType = edmNavigationProperty.getType();
        if (!edmNavigationProperty.containsTarget()) {
          // contextURL displays the last segment
          responseEdmEntitySet = Util.getNavigationTargetEntitySet(startEdmEntitySet, edmNavigationProperty);
        } else {
          responseEdmEntitySet = startEdmEntitySet;
        }


        // 2nd: fetch the data from backend.
        // e.g. for the URI: Products(1)/Category we have to find the correct Category entity
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        // e.g. for Products(1)/Category we have to find first the Products(1)
        Entity sourceEntity = ToopDirClient.readEntityData(startEdmEntitySet, keyPredicates);

        // now we have to check if the navigation is
        // a) to-one: e.g. Products(1)/Category
        // b) to-many with key: e.g. Categories(3)/Products(5)
        // the key for nav is used in this case: Categories(3)/Products(5)
        List<UriParameter> navKeyPredicates = uriResourceNavigation.getKeyPredicates();

        if (navKeyPredicates.isEmpty()) { // e.g. DemoService.svc/Products(1)/Category
          responseEntity = ToopDirClient.getRelatedEntity(sourceEntity, responseEdmEntityType);
        } else { // e.g. DemoService.svc/Categories(3)/Products(5)
          responseEntity = ToopDirClient.getRelatedEntity(sourceEntity, responseEdmEntityType, navKeyPredicates);
        }
      }
    } else {
      // this would be the case for e.g. Products(1)/Category/Products(1)/Category
      throw new ODataApplicationException("Not supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    if (responseEntity == null) {
      // this is the case for e.g. DemoService.svc/Categories(4) or DemoService.svc/Categories(3)/Products(999)
      throw new ODataApplicationException("Nothing found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
    }

    // 3. serialize
    ContextURL contextUrl = null;
    if (

        isContNav(uriInfo)) {
      contextUrl = ContextURL.with().entitySetOrSingletonOrType(request.getRawODataPath()).
          suffix(Suffix.ENTITY).build();
    } else {
      contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).suffix(Suffix.ENTITY).build();
    }

    EntitySerializerOptions opts = EntitySerializerOptions.with().contextURL(contextUrl).build();

    ODataSerializer serializer = this.odata.createSerializer(responseFormat);
    SerializerResult serializerResult = serializer.entity(this.serviceMetadata, responseEdmEntityType, responseEntity, opts);

    // 4. configure the response object
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
