package eu.toop.odata;


import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorProcessor implements org.apache.olingo.server.api.processor.ErrorProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorProcessor.class);

  @Override
  public void init(OData odata, ServiceMetadata serviceMetadata) {
  }

  @Override
  public void processError(ODataRequest request, ODataResponse response, ODataServerError serverError, ContentType responseFormat) {
    LOGGER.error(" server error " + serverError.getException().getMessage(), serverError.getException());
  }
}
