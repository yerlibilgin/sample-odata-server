package eu.toop.model.entity;

import eu.toop.model.complex.ToProperty;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public abstract class IdentifierTypeWrapper extends CsdlEntityType implements ToProperty {
  public IdentifierTypeWrapper() {
    //setKey(Arrays.asList(
    //    //new CsdlPropertyRef().setName("scheme"),
    //    new CsdlPropertyRef().setName("value")
    //));

    setProperties(
        Arrays.asList(
            new CsdlProperty().setName("scheme").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
            new CsdlProperty().setName("value").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
        ));
  }


  private String scheme;
  private String value;


  public IdentifierTypeWrapper(String scheme, String value) {
    this();
    this.scheme = scheme;
    this.value = value;
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }


  @Override
  public Property toProperty() {
    Entity entity = new Entity();
    //entity.setId(createId());
    List<Property> pro = entity.getProperties();
    pro.add(createPrimitive("scheme", getScheme()));
    pro.add(createPrimitive("value", getValue()));

    System.out.println("\tthis.getClass " + this.getClass() + " " + this.getName());
    Property property = new Property(null, this.getName(), ValueType.ENTITY, entity);
    return property;
  }


  public static final Property createPrimitive(final String name, final Object value) {
    return new Property(null, name, ValueType.PRIMITIVE, value);
  }

  public static final URI createId(String entitySetName, Object id) {
    try {
      URI uri = new URI(entitySetName + "(" + id + ")");
      System.out.println("uri: " + uri);
      return uri;
    } catch (URISyntaxException e) {
      throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
    }
  }
}
