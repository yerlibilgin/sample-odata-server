package eu.toop.model;

import eu.peppol.schema.pd.businesscard_generic._201907.BusinessCardType;
import eu.toop.Util;
import eu.toop.model.entity.EdmStructure;
import eu.toop.model.entity.ODataDirectoryHelper;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BusinessCardTypeWrapper extends Entity {
  private final int id;
  private final BusinessCardType businessCardType;
  private List<DocTypeWrapper> docTypeWrapperList = new ArrayList<>();

  public BusinessCardTypeWrapper(int bcId, BusinessCardType businessCardType) {
    this.id = bcId;
    this.businessCardType = businessCardType;

    setType(EdmStructure.FQN_BusinessCard.getFullQualifiedNameAsString());
    setId(Util.createId());
    addProperty(Util.createPrimitive("Id", id));

    //participant = new ComplexValue();
    //participant.setTypeName(EdmStructure.NAME_Participant);
    //participant.
//
    //    addProperty(new Property())
    ////entity.addProperty(ODataDirectoryHelper.createPrimitive("scheme", idType.getScheme()));
    //entity.addProperty(ODataDirectoryHelper.createPrimitive("value", idType.getScheme()));

  }



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

  public BusinessCardType getBusinessCardType() {
    return businessCardType;
  }

  public List<DocTypeWrapper> getDocTypes() {
    return docTypeWrapperList;
  }
}
