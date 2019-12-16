package eu.toop;


import eu.toop.model.complex.ServiceInformationTypeWrapper;
import eu.toop.model.entity.CountryAwareServiceMetadataTypeWrapper;
import eu.toop.model.entity.DocTypeIdentifierWrapper;
import eu.toop.model.entity.ParticipantIdentifierTypeWrapper;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.ex.ODataNotSupportedException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.*;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
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

  @Override
  public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    UriInfoResource uriInfoResource = uriInfo.asUriInfoResource();
    LOGGER.debug("readEntityCollection " + uriInfoResource);

    EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfoResource);

    LOGGER.debug("EDM ENTITY SET: " + edmEntitySet);

    // Second we fetch the data for this specific entity set from the mock database and transform it into an EntitySet
    // object which is understood by our serialization

    //DiscoveryProvider.getInstance().getAllParticipantIDs();
    EntityCollection entitySet = new EntityCollection();
    ParticipantIdentifierTypeWrapper pid = new ParticipantIdentifierTypeWrapper("scheme1", "party1");
    DocTypeIdentifierWrapper did = new DocTypeIdentifierWrapper("scheme1", "docType1");
    ServiceInformationTypeWrapper si = new ServiceInformationTypeWrapper(pid, did);

    CountryAwareServiceMetadataTypeWrapper country = new CountryAwareServiceMetadataTypeWrapper("TR", si);
    entitySet.getEntities().add(country.toEntity());


    country = new CountryAwareServiceMetadataTypeWrapper("ES", si);
    entitySet.getEntities().add(country.toEntity());


    // Next we create a serializer based on the requested format. This could also be a custom format but we do not
    // support them in this example
    ODataSerializer serializer = odata.createSerializer(responseFormat);

    LOGGER.error("Serializer: " + serializer);

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
  public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    LOGGER.debug("readEntity xxxxxxxxxxxxxxxxxxxxxxx " + uriInfo.toString());
  }

  @Override
  public void readPrimitiveValue(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    LOGGER.debug("readPrimitiveValue " + uriInfo.toString());
  }

  @Override
  public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
    LOGGER.debug("readPrimitive " + uriInfo.toString());
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
