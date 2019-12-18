package eu.toop.model;

import eu.peppol.schema.pd.businesscard_generic._201907.BusinessCardType;
import eu.toop.Util;
import eu.toop.model.entity.EdmStructure;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;

import java.util.ArrayList;
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

    ComplexValue complexValue = new ComplexValue();
    List<Property> pro = complexValue.getValue();
    pro.add(Util.createPrimitive("scheme", businessCardType.getParticipant().getScheme()));
    pro.add(Util.createPrimitive("value", businessCardType.getParticipant().getScheme()));

    Property property = new Property(null, EdmStructure.NAME_Participant, ValueType.COMPLEX, complexValue);
    addProperty(property);


    complexValue = new ComplexValue();
    pro = complexValue.getValue();
    pro.add(Util.createPrimitive("countrycode", businessCardType.getEntity().get(0).getCountrycode()));
    pro.add(Util.createPrimitive("name", businessCardType.getEntity().get(0).getName().get(0).getName()));
    //if (businessCardType.getEntity().get(0).getId().size() > 0) {
    //  pro.add(Util.createPrimitive("scheme", businessCardType.getEntity().get(0).getId().get(0).getScheme()));
    //  pro.add(Util.createPrimitive("value", businessCardType.getEntity().get(0).getId().get(0).getValue()));
    //}

    property = new Property(null, EdmStructure.NAME_Entity, ValueType.COMPLEX, complexValue);
    addProperty(property);

  }

  public BusinessCardType getBusinessCardType() {
    return businessCardType;
  }

  public List<DocTypeWrapper> getDocTypes() {
    return docTypeWrapperList;
  }
}
