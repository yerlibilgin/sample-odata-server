package eu.toop.model.complex;

import eu.toop.DiscoveryEdmProvider;
import eu.toop.model.entity.DocTypeIdentifierWrapper;
import eu.toop.model.entity.IdentifierTypeWrapper;
import eu.toop.model.entity.ParticipantIdentifierTypeWrapper;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

import java.util.Arrays;
import java.util.List;

/**
 * <pre>
 * <bdxr:ServiceInformation>
 * <bdxr:ParticipantIdentifier scheme="iso6523-actorid-upis">9999:elonia-dev</bdxr:ParticipantIdentifier>
 * <bdxr:DocumentIdentifier scheme="toop-doctypeid-qns">
 * urn:eu:toop:ns:dataexchange-1p40::Request##urn:eu.toop.request.registeredorganization::1.40
 * </bdxr:DocumentIdentifier>
 * <!-- the rest is not included as it is smp related -->
 * </bdxr:ServiceInformation>
 * </pre>
 */
public class ServiceInformationTypeWrapper extends CsdlComplexType implements ToProperty {
  public static final String ET_NAME = "ServiceInformation";
  public static final FullQualifiedName FQN =
      new FullQualifiedName(DiscoveryEdmProvider.NAMESPACE, ServiceInformationTypeWrapper.ET_NAME);

  private ParticipantIdentifierTypeWrapper pid;
  private DocTypeIdentifierWrapper did;

  public ServiceInformationTypeWrapper() {
    setName(ET_NAME);

    setProperties(
        Arrays.asList(
            new CsdlProperty().setName(ParticipantIdentifierTypeWrapper.ET_NAME).setType(
                ParticipantIdentifierTypeWrapper.FQN),
            new CsdlProperty().setName(DocTypeIdentifierWrapper.ET_NAME).setType(
                DocTypeIdentifierWrapper.FQN)
        ));
  }


  public ServiceInformationTypeWrapper(ParticipantIdentifierTypeWrapper pid, DocTypeIdentifierWrapper did) {
    this();
    this.pid = pid;
    this.did = did;
  }

  public static String getFullName() {
    return FQN.getFullQualifiedNameAsString();
  }

  @Override
  public Property toProperty() {
    ComplexValue complexValue = new ComplexValue();
    List<Property> pro = complexValue.getValue();
    pro.add(pid.toProperty());
    pro.add(did.toProperty());

    Property property = new Property(null, ET_NAME, ValueType.COMPLEX, complexValue);
    return property;
  }


}
