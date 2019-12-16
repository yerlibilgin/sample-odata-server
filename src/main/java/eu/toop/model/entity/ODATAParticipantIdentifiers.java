package eu.toop.model.entity;

import eu.toop.DiscoveryEdmProvider;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;

public class ODATAParticipantIdentifiers extends CsdlEntitySet {
  public static final String ET_NAME = "ParticipantIdentifiers";

  public static final FullQualifiedName FQN = new FullQualifiedName(DiscoveryEdmProvider.NAMESPACE, ET_NAME);
  private static ODATAParticipantIdentifiers instance = new ODATAParticipantIdentifiers();

  private ODATAParticipantIdentifiers() {
    setName(ET_NAME);
    setType(ODATAParticipantIdentifier.FQN);
  }

  public static final ODATAParticipantIdentifiers getInstance(){
    return instance;
  }
}
