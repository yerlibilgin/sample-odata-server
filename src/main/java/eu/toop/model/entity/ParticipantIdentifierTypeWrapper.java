package eu.toop.model.entity;

import eu.toop.DiscoveryEdmProvider;
import org.apache.olingo.commons.api.edm.FullQualifiedName;

public class ParticipantIdentifierTypeWrapper extends IdentifierTypeWrapper {
  public static final String ET_NAME = "ParticipantIdentifier";

  public static final FullQualifiedName FQN = new FullQualifiedName(DiscoveryEdmProvider.NAMESPACE, ET_NAME);

  public ParticipantIdentifierTypeWrapper() {
    setName(ET_NAME);
  }

  public ParticipantIdentifierTypeWrapper(String scheme, String value) {
    super(scheme, value);
    setName(ET_NAME);
  }
}
