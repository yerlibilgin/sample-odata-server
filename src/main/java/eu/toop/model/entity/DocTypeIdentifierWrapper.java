package eu.toop.model.entity;

import eu.toop.DiscoveryEdmProvider;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.ext.proxy.api.annotations.EntityType;

@EntityType(name = "DocumentTypeIdentifier")
public class DocTypeIdentifierWrapper extends IdentifierTypeWrapper {
  public static final String ET_NAME = "DocumentTypeIdentifier";
  public static final FullQualifiedName FQN = new FullQualifiedName(DiscoveryEdmProvider.NAMESPACE, ET_NAME);

  public DocTypeIdentifierWrapper() {
    setName(ET_NAME);
  }

  public DocTypeIdentifierWrapper(String scheme, String value) {
    super(scheme, value);
    setName(ET_NAME);
  }
}
