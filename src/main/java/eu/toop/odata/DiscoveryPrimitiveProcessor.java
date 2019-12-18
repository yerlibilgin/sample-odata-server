package eu.toop.odata;


import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.ex.ODataNotSupportedException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class DiscoveryPrimitiveProcessor implements PrimitiveProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryPrimitiveProcessor.class);

  private OData odata;
  private ServiceMetadata serviceMetadata;

  @Override
  public void init(OData odata, ServiceMetadata serviceMetadata) {
    this.odata = odata;
    this.serviceMetadata = serviceMetadata;
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
    UriResourceProperty uriProperty = (UriResourceProperty)resourceParts.get(resourceParts.size() -1);
    EdmProperty edmProperty = uriProperty.getProperty();
    String edmPropertyName = edmProperty.getName();
    // in our example, we know we have only primitive types in our model
    EdmPrimitiveType edmPropertyType = (EdmPrimitiveType) edmProperty.getType();


    // 2. retrieve data from backend
    // 2.1. retrieve the entity data, for which the property has to be read
    Entity entity = ToopDirClient.readEntityData(edmEntitySet, keyPredicates);
    if (entity == null) { // Bad request
      throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
    }

    // 2.2. retrieve the property data from the entity
    Property property = entity.getProperty(edmPropertyName);
    if (property == null) {
      throw new ODataApplicationException("Property not found", HttpStatusCode.NOT_FOUND.getStatusCode(),
          Locale.ENGLISH);
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

      //4. configure the response object
      response.setContent(propertyStream);
      response.setStatusCode(HttpStatusCode.OK.getStatusCode());
      response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    } else {
      // in case there's no value for the property, we can skip the serialization
      response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }
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

}
