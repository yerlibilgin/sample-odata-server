package eu.toop.model.entity;

import eu.toop.DiscoveryEdmProvider;
import eu.toop.model.complex.ServiceInformationTypeWrapper;
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

public class CountryAwareServiceMetadataTypeWrapper extends CsdlEntityType {
  public static final String ET_NAME = "CountryAwareServiceMetadata";
  public static final FullQualifiedName FQN = new FullQualifiedName(DiscoveryEdmProvider.NAMESPACE, ET_NAME);
  private String countryCode;
  private ServiceInformationTypeWrapper serviceInformation;

  public CountryAwareServiceMetadataTypeWrapper() {
    setName(ET_NAME);

    setKey(Arrays.asList(
        new CsdlPropertyRef().setName("countryCode")
    ));

    setProperties(
        Arrays.asList(
            new CsdlProperty().setName("countryCode").setType(
                EdmPrimitiveTypeKind.String.getFullQualifiedName()),
            new CsdlProperty().setName(ServiceInformationTypeWrapper.ET_NAME).setType(ServiceInformationTypeWrapper.FQN))
    );

  }

  public CountryAwareServiceMetadataTypeWrapper(String countryCode, ServiceInformationTypeWrapper serviceInformation) {
    this();

    this.countryCode = countryCode;
    this.serviceInformation = serviceInformation;
  }



  public static String getFullName() {
    return FQN.getFullQualifiedNameAsString();
  }

  public Entity toEntity() {
    Entity e = new Entity();
    e.addProperty(new Property(null, "countryCode", ValueType.PRIMITIVE, countryCode));
    e.addProperty(serviceInformation.toProperty());
    e.setId(createId("ServiceMetadataList", countryCode));
    e.setType(CountryAwareServiceMetadataTypeWrapper.getFullName());
    return e;
  }


  private URI createId(String entitySetName, Object id) {
    try {
      URI uri = new URI(entitySetName + "(" + id + ")");
      System.out.println("uri: " + uri);
      return uri;
    } catch (URISyntaxException e) {
      throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
    }
  }
}
