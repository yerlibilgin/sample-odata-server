package eu.toop.model.entity;

import com.helger.peppolid.IParticipantIdentifier;
import eu.toop.DiscoveryEdmProvider;
import org.apache.http.client.utils.URIBuilder;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class ODATAParticipantIdentifier extends CsdlEntityType {
  public static final String ET_NAME = "ParticipantIdentifier";

  public static final FullQualifiedName FQN = new FullQualifiedName(DiscoveryEdmProvider.NAMESPACE, ET_NAME);
  private String scheme;
  private String value;
  private String uriEncoded;


  public ODATAParticipantIdentifier() {
    setName(ET_NAME);

    setKey(Arrays.asList(
        new CsdlPropertyRef().setName("scheme"),
        new CsdlPropertyRef().setName("value")
    ));

    setProperties(
        Arrays.asList(
            new CsdlProperty().setName("scheme").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
            new CsdlProperty().setName("value").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
            new CsdlProperty().setName("uriEncoded").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
        ));

  }

  public ODATAParticipantIdentifier(IParticipantIdentifier idp) {
    this();

    this.scheme = idp.getScheme();
    this.value = idp.getValue();
    this.uriEncoded = idp.getURIEncoded();
  }

  public String getScheme() {
    return scheme;
  }

  public String getValue() {
    return value;
  }

  public String getUriEncoded() {
    return uriEncoded;
  }

  public void setUriEncoded(String uriEncoded) {
    this.uriEncoded = uriEncoded;
  }

  public Entity asEntity() {
    Entity entity = new Entity();
    entity.setId(createId());
    List<Property> pro = entity.getProperties();
    pro.add(createPrimitive("scheme", getScheme()));
    pro.add(createPrimitive("value", getValue()));
    pro.add(createPrimitive("uriEncoded", getUriEncoded()));

    System.out.println("\tthis.getClass " + this.getClass() + " " + this.getName());
    return entity;
  }


  public static final Property createPrimitive(final String name, final Object value) {
    return new Property(null, name, ValueType.PRIMITIVE, value);
  }

  public static final URI createId() {
    try {
      URIBuilder uriBuilder = new URIBuilder();
      URI uri = new URI(ODATAParticipantIdentifiers.ET_NAME + "(scheme,value)");
      System.out.println("uri: " + uri);
      return uri;
    } catch (URISyntaxException e) {
      throw new ODataRuntimeException("Unable to create id for entity: " + ODATAParticipantIdentifiers.ET_NAME, e);
    }
  }
}
