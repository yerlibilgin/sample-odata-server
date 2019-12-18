package eu.toop.odata;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryEdmProvider extends CsdlAbstractEdmProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryEdmProvider.class);


  @Override
  public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) {
    LOGGER.debug("ENTITY: " + entityTypeName.getFullQualifiedNameAsString());
    if (EdmStructure.FQN_BusinessCard.equals(entityTypeName)) {
      return EdmStructure.ET_BusinessCard;
    } else if (EdmStructure.FQN_DoctypeID.equals(entityTypeName)){
      return EdmStructure.ET_DoctypeID;
    }

    return null;
  }

  @Override
  public CsdlComplexType getComplexType(final FullQualifiedName complexTypeName) {
    LOGGER.debug("ComplexType : " + complexTypeName.getFullQualifiedNameAsString());

    if (EdmStructure.FQN_Entity.equals(complexTypeName)) {
      return EdmStructure.CT_Entity;
    } else if (EdmStructure.FQN_Participant.equals(complexTypeName)) {
      return EdmStructure.CT_Participant;
    }

    return null;
  }


  @Override
  public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName) {
    if (entityContainer != null)
      LOGGER.debug("ENTITY SET: " + entityContainer.getFullQualifiedNameAsString() + " " + entitySetName);
    else
      LOGGER.debug("ENTITY SET: null" + " " + entitySetName);

    if (EdmStructure.CONTAINER_FQN.equals(entityContainer)){
      if(EdmStructure.NAME_BusinessCards.equals(entitySetName)) {
        return EdmStructure.ES_BusinessCards;
      }
    }

    return null;
  }

  @Override
  public List<CsdlSchema> getSchemas() {
    List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
    CsdlSchema schema = new CsdlSchema();
    schema.setNamespace(EdmStructure.NAMESPACE);
    // EntityTypes
    List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
    entityTypes.add(EdmStructure.ET_BusinessCard);
    entityTypes.add(EdmStructure.ET_DoctypeID);
    schema.setEntityTypes(entityTypes);

    //// ComplexTypes
    // we don't have complex types for now
    List<CsdlComplexType> complexTypes = new ArrayList<>();
    complexTypes.add(EdmStructure.CT_Entity);
    complexTypes.add(EdmStructure.CT_Participant);
    schema.setComplexTypes(complexTypes);

    // EntityContainer
    schema.setEntityContainer(getEntityContainer());
    schemas.add(schema);

    return schemas;
  }

  @Override
  public CsdlEntityContainer getEntityContainer() {
    CsdlEntityContainer container = new CsdlEntityContainer();
    container.setName(EdmStructure.CONTAINER_FQN.getName());

    // EntitySets
    List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
    container.setEntitySets(entitySets);
    entitySets.add(EdmStructure.ES_BusinessCards);
    return container;
  }

  @Override
  public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName) {
    LOGGER.debug("Entity container info " + entityContainerName);
    if (entityContainerName == null || EdmStructure.CONTAINER_FQN.equals(entityContainerName)) {
      return EdmStructure.ECI_Container;
    }
    return null;
  }
}
