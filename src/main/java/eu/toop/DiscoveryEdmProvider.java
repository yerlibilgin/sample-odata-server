package eu.toop;

import eu.toop.model.complex.ServiceInformationTypeWrapper;
import eu.toop.model.entity.CountryAwareServiceMetadataTypeWrapper;
import eu.toop.model.entity.DocTypeIdentifierWrapper;
import eu.toop.model.entity.ParticipantIdentifierTypeWrapper;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscoveryEdmProvider extends CsdlAbstractEdmProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryEdmProvider.class);

  // Service Namespace
  public static final String NAMESPACE = "eu.toop.dsd.odata";


  // EDM Container
  public static final String CONTAINER_NAME = "Container";
  public static final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

  /*
  EntityTypes (as they have a KEY that resolves to them)
  CountryAwareServiceMetadata
  ParticipantIdentifier
  DocumentIdentifier
   */
  /*
  Complex Types: (they don't have a key)
  ServiceMetadata
  ServiceInformation
  ServiceMetadataList
   */


/*
<sim:ServiceMetadataList xmlns:bdxr="http://docs.oasis-open.org/bdxr/ns/SMP/2016/05"
                         xmlns:sim="http://eu/toop/simulator/schema/discovery">


  <sim:CountryAwareServiceMetadata countryCode="GQ">
      <bdxr:ServiceInformation>
        <bdxr:ParticipantIdentifier scheme="iso6523-actorid-upis">9999:elonia-dev</bdxr:ParticipantIdentifier>
        <bdxr:DocumentIdentifier scheme="toop-doctypeid-qns">
          urn:eu:toop:ns:dataexchange-1p40::Request##urn:eu.toop.request.registeredorganization::1.40
        </bdxr:DocumentIdentifier>
        <!-- the rest is not included as it is smp related -->
      </bdxr:ServiceInformation>
  </sim:CountryAwareServiceMetadata>
    ..
</sim:ServiceMetadataList>
 */

  private static final Map<FullQualifiedName, CsdlEntityType> csdlEntityMap = new HashMap<>();
  private static final Map<FullQualifiedName, CsdlComplexType> csdlComplexTypeMap = new HashMap<>();
  private static final String ES_SERVICE_METADATA_LIST = "ServiceMetadataList";

  static {
    csdlEntityMap.put(CountryAwareServiceMetadataTypeWrapper.FQN, new CountryAwareServiceMetadataTypeWrapper());
    csdlEntityMap.put(ParticipantIdentifierTypeWrapper.FQN, new ParticipantIdentifierTypeWrapper());
    csdlEntityMap.put(DocTypeIdentifierWrapper.FQN, new DocTypeIdentifierWrapper());

    csdlComplexTypeMap.put(ServiceInformationTypeWrapper.FQN, new ServiceInformationTypeWrapper());

  }

  @Override
  public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) {
    if (entityTypeName != null) {
      LOGGER.debug("ENTITY: " + entityTypeName.getFullQualifiedNameAsString());
      return csdlEntityMap.get(entityTypeName);
    }

    return null;
  }

  @Override
  public CsdlComplexType getComplexType(final FullQualifiedName complexTypeName) {
    if (complexTypeName != null) {
      LOGGER.debug("CTTYPE:" + complexTypeName.getFullQualifiedNameAsString());
      CsdlComplexType csdlComplexType = csdlComplexTypeMap.get(complexTypeName);
      LOGGER.debug("  \t" + csdlComplexType);
      return csdlComplexType;
    }

    return null;
  }


  @Override
  public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName) {
    if (entityContainer != null)
      LOGGER.debug("ENTITY SET: " + entityContainer.getFullQualifiedNameAsString() + " " + entitySetName);
    else
      LOGGER.debug("ENTITY SET: null" + " " + entitySetName);

    if (CONTAINER_FQN.equals(entityContainer)) {
      if (ES_SERVICE_METADATA_LIST.equals(entitySetName)) {
        return new CsdlEntitySet()
            .setName(ES_SERVICE_METADATA_LIST)
            .setType(CountryAwareServiceMetadataTypeWrapper.FQN);
      } else {
        LOGGER.debug("Unfortunately returning null " + ES_SERVICE_METADATA_LIST);
      }
    } else {
      LOGGER.debug("Unfortunately returning null");
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
    entityTypes.addAll(csdlEntityMap.values());
    schema.setEntityTypes(entityTypes);

    // ComplexTypes
    List<CsdlComplexType> complexTypes = new ArrayList<CsdlComplexType>();
    complexTypes.addAll(csdlComplexTypeMap.values());
    schema.setComplexTypes(complexTypes);

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
    entitySets.add(new CsdlEntitySet()
        .setName(ES_SERVICE_METADATA_LIST)
        .setType(CountryAwareServiceMetadataTypeWrapper.FQN));
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
