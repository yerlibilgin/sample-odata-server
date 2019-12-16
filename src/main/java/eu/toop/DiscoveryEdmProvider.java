package eu.toop;

import eu.toop.model.entity.ODATAParticipantIdentifier;
import eu.toop.model.entity.ODATAParticipantIdentifiers;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryEdmProvider extends CsdlAbstractEdmProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryEdmProvider.class);

  // Service Namespace
  public static final String NAMESPACE = "eu.toop.dsd.odata";


  // EDM Container
  public static final String CONTAINER_NAME = "Container";
  public static final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

  @Override
  public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) {
    LOGGER.debug("ENTITY: " + entityTypeName.getFullQualifiedNameAsString());
    if (ODATAParticipantIdentifier.FQN.equals(entityTypeName)) {
      return new ODATAParticipantIdentifier();
    }

    return null;
  }

  // we don't have a complex type, no need to override this
  //@Override
  //public CsdlComplexType getComplexType(final FullQualifiedName complexTypeName) {
  //  return null;
  //}


  @Override
  public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName) {
    if (entityContainer != null)
      LOGGER.debug("ENTITY SET: " + entityContainer.getFullQualifiedNameAsString() + " " + entitySetName);
    else
      LOGGER.debug("ENTITY SET: null" + " " + entitySetName);

    if (CONTAINER_FQN.equals(entityContainer) &&
        ODATAParticipantIdentifiers.ET_NAME.equals(entitySetName)) {
        return ODATAParticipantIdentifiers.getInstance();
    }

    return null;
  }

  @Override
  public List<CsdlSchema> getSchemas() {
    List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
    CsdlSchema schema = new CsdlSchema();
    schema.setNamespace(NAMESPACE);
    // EntityTypes
    List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
    entityTypes.add(new ODATAParticipantIdentifier());

    schema.setEntityTypes(entityTypes);

    //// ComplexTypes
    // we don't have complex types for now

    // EntityContainer
    schema.setEntityContainer(getEntityContainer());
    schemas.add(schema);

    return schemas;
  }

  @Override
  public CsdlEntityContainer getEntityContainer() {
    CsdlEntityContainer container = new CsdlEntityContainer();
    container.setName(CONTAINER_FQN.getName());

    // EntitySets
    List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
    container.setEntitySets(entitySets);
    entitySets.add(ODATAParticipantIdentifiers.getInstance());
    return container;
  }

  @Override
  public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName) {
    LOGGER.debug("Entity container info " + entityContainerName);
    if (entityContainerName == null || CONTAINER_FQN.equals(entityContainerName)) {
      return new CsdlEntityContainerInfo().setContainerName(CONTAINER_FQN);
    }
    return null;
  }
}
