package eu.toop.model.entity;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;

import java.util.Arrays;

public class EdmStructure {
  // Service Namespace
  public static final String NAMESPACE = "eu.toop.dsd.odata";

  // EDM Container
  public static final String CONTAINER_NAME = "Container";
  public static final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);


  //Entity Names
  public static final String NAME_BusinessCards = "BusinessCards";
  public static final String NAME_BusinessCard = "BusinessCard";
  public static final String NAME_Participant = "Participant";
  public static final String NAME_Entity = "Entity";
  public static final String NAME_DoctypeID = "DoctypeID";
  public static final String NAME_DoctypeIDs = "DoctypeIDs";

  //FQNs
  public static final FullQualifiedName FQN_BusinessCards = new FullQualifiedName(NAMESPACE, NAME_BusinessCards);
  public static final FullQualifiedName FQN_DoctypesIDS = new FullQualifiedName(NAMESPACE, NAME_DoctypeIDs);

  public static final FullQualifiedName FQN_BusinessCard = new FullQualifiedName(NAMESPACE, NAME_BusinessCard);
  public static final FullQualifiedName FQN_Participant = new FullQualifiedName(NAMESPACE, NAME_Participant);
  public static final FullQualifiedName FQN_Entity = new FullQualifiedName(NAMESPACE, NAME_Entity);
  public static final FullQualifiedName FQN_DoctypeID = new FullQualifiedName(NAMESPACE, NAME_DoctypeID);


  public static final CsdlEntityContainerInfo ECI_Container = new CsdlEntityContainerInfo().setContainerName(EdmStructure.CONTAINER_FQN);;

  public static final CsdlEntitySet ES_BusinessCards = new CsdlEntitySet().setName(NAME_BusinessCards).setType(FQN_BusinessCard);

  public static final CsdlEntityType ET_BusinessCard = new CsdlEntityType().setName(NAME_BusinessCard).setKey(
      Arrays.asList(
          new CsdlPropertyRef().setName("Id")
      )
  ).setProperties(
      Arrays.asList(
          new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName()),
          new CsdlProperty().setName(NAME_Participant).setType(FQN_Participant),
          new CsdlProperty().setName(NAME_Entity).setType(FQN_Entity)
      )
  ).setNavigationProperties(
      Arrays.asList(
          new CsdlNavigationProperty().setName(NAME_DoctypeIDs).setType(FQN_DoctypesIDS).setNullable(Boolean.TRUE)
      )
  );

  public static final CsdlComplexType CT_Participant = new CsdlComplexType().setName(NAME_Participant).setProperties(
      Arrays.asList(
          new CsdlProperty().setName("scheme").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
          new CsdlProperty().setName("value").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
      )
  );

  public static final CsdlComplexType CT_Entity = new CsdlComplexType().setName(NAME_Entity).setProperties(
      Arrays.asList(
          new CsdlProperty().setName("countrycode").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
          new CsdlProperty().setName("name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
          new CsdlProperty().setName("scheme").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
          new CsdlProperty().setName("value").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
      )
  );

  public static final CsdlEntitySet ES_DoctypeIDS = new CsdlEntitySet().setName(NAME_DoctypeIDs).setType(FQN_DoctypeID);

  public static final CsdlEntityType ET_DoctypeID = new CsdlEntityType().setName(NAME_DoctypeID).setKey(
      Arrays.asList(
          new CsdlPropertyRef().setName("Id")
      )
  ).setProperties(
      Arrays.asList(
          new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName()),
          new CsdlProperty().setName("scheme").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
          new CsdlProperty().setName("value").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
      )
  ).setNavigationProperties(
      Arrays.asList(
          new CsdlNavigationProperty().setName(NAME_BusinessCards).setType(FQN_BusinessCards).setNullable(Boolean.TRUE)
      )
  );
}
