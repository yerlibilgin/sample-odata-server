package eu.toop.model;

import eu.peppol.schema.pd.businesscard_generic._201907.IDType;
import eu.toop.Util;
import eu.toop.model.entity.EdmStructure;
import org.apache.olingo.commons.api.data.Entity;

public class DocTypeWrapper extends Entity {


  private final int id;

  private String scheme;
  private String value;

  public DocTypeWrapper(int docId, IDType idType) {
    this.id = docId;

    this.setId(Util.createId());
    this.scheme = idType.getScheme();
    this.value = idType.getValue();

    this.getProperties().add(Util.createPrimitive("Id", id));
    this.getProperties().add(Util.createPrimitive("scheme", scheme));
    this.getProperties().add(Util.createPrimitive("value", value));
    this.setType(EdmStructure.FQN_DoctypeID.getFullQualifiedNameAsString());
  }

  public String getScheme() {
    return scheme;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
