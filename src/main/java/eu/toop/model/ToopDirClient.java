/**
 * Copyright (C) 2018-2019 toop.eu
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.toop.model;

import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerJson;
import com.helger.json.IJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import eu.peppol.schema.pd.businesscard_generic._201907.ObjectFactory;
import eu.peppol.schema.pd.businesscard_generic._201907.RootType;
import eu.toop.Util;
import eu.toop.model.entity.EdmStructure;
import org.apache.http.client.methods.HttpGet;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.server.api.uri.UriParameter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ToopDirClient {
  private static final int MAX_RESULTS_PER_PAGE = 100;

  private static final String m_sBaseURL = "http://directory.acc.exchange.toop.eu";
  //private static final String TOOP_DIR_EXPORT_URL = "http://directory.acc.exchange.toop.eu/export/businesscards";
  private static final String TOOP_DIR_EXPORT_URL = "file:./directory-export-business-cards.xml";

  private static final Map<Integer, BusinessCardTypeWrapper> businessCards = new LinkedHashMap<>();
  private static final Map<Integer, DocTypeWrapper> docTypeMap = new LinkedHashMap<>();

  static {
    getAllParticipantIDs();
  }

  private static IJsonObject _fetchJsonObject(final HttpClientManager aMgr,
                                              final ISimpleURL aURL) throws IOException {
    final HttpGet aGet = new HttpGet(aURL.getAsURI());
    final ResponseHandlerJson aRH = new ResponseHandlerJson();
    final IJson aJson = aMgr.execute(aGet, aRH);
    if (aJson != null && aJson.isObject())
      return aJson.getAsObject();

    return null;
  }



  public static ICommonsSet<IParticipantIdentifier> getAllParticipantIDs(final String sCountryCode,
                                                                         final IDocumentTypeIdentifier aDocumentTypeID) {
    final ICommonsSet<IParticipantIdentifier> ret = new CommonsHashSet<>();

    final HttpClientFactory aHCFactory = new HttpClientFactory();

    try (final HttpClientManager aMgr = new HttpClientManager(aHCFactory)) {
      // Build base URL and fetch x records per HTTP request
      final SimpleURL aBaseURL = new SimpleURL(m_sBaseURL + "/search/1.0/json")
          .add("rpc", MAX_RESULTS_PER_PAGE);

      if (sCountryCode != null)
        aBaseURL.add("country", sCountryCode);

      if (aDocumentTypeID != null)
        aBaseURL.add("doctype",
            aDocumentTypeID.getURIEncoded());

      // Fetch first object
      IJsonObject aResult = _fetchJsonObject(aMgr, aBaseURL);
      if (aResult != null) {
        // Start querying results
        int nResultPageIndex = 0;
        int nLoops = 0;
        while (true) {
          int nMatchCount = 0;
          final IJsonArray aMatches = aResult.getAsArray("matches");
          if (aMatches != null) {
            for (final IJson aMatch : aMatches) {
              ++nMatchCount;
              final IJsonObject aID = aMatch.getAsObject().getAsObject("participantID");
              if (aID != null) {
                final String sScheme = aID.getAsString("scheme");
                final String sValue = aID.getAsString("value");
                final IParticipantIdentifier aPI = new SimpleParticipantIdentifier(sScheme, sValue);
                if (aPI != null)
                  ret.add(aPI);
                else
                  System.err.println("Errorr");
              } else
                System.err.println("Match does not contain participant ID");
            }
          } else
            System.err.println("JSON response contains no 'matches'");

          if (nMatchCount < MAX_RESULTS_PER_PAGE) {
            // Got less results than expected - end of list
            break;
          }

          if (++nLoops > MAX_RESULTS_PER_PAGE) {
            // Avoid endless loop
            System.err.println("Endless loop in PD fetching?");
            break;
          }

          // Query next page
          nResultPageIndex++;
          aResult = _fetchJsonObject(aMgr, aBaseURL.getClone().add("rpi", nResultPageIndex));
          if (aResult == null) {
            // Unexpected error - stop querying
            // Error was already logged
            break;
          }
        }
      }
    } catch (final IOException ex) {
      ex.printStackTrace();
    }

    return ret;
  }



  public static Map<Integer, BusinessCardTypeWrapper> getAllParticipantIDs() {

    if (businessCards.size() == 0) {
      try {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        final JAXBElement<RootType> unmarshal = (JAXBElement<RootType>) jaxbUnmarshaller.unmarshal(new URL(TOOP_DIR_EXPORT_URL));
        RootType rootType = unmarshal.getValue();

        final AtomicInteger idEr = new AtomicInteger(0);
        final AtomicInteger docEr = new AtomicInteger(0);
        //process the entities
        rootType.getBusinesscard().forEach(businessCardType -> {
          final int bcId = idEr.getAndIncrement();
          final BusinessCardTypeWrapper bcw = new BusinessCardTypeWrapper(bcId, businessCardType);
          businessCards.put(bcId, bcw);

          businessCardType.getDoctypeid().forEach(idType -> {
            final int docId = docEr.getAndIncrement();

            final DocTypeWrapper docTypeWrapper = new DocTypeWrapper(docId, idType);
            bcw.getDocTypes().add(docTypeWrapper);

            docTypeMap.put(docId, docTypeWrapper);
          });
        });

      } catch (Exception ex) {
        throw new IllegalStateException("Sorry Cannot read directory.", ex);
      }
    }

    return businessCards;
  }

  @Override
  public String toString() {
    return new ToStringGenerator(this).append("BaseURL", m_sBaseURL).getToString();
  }

  public static EntityCollection getRelatedEntityCollection(Entity sourceEntity, EdmEntityType targetEntityType) {
      EntityCollection navigationTargetEntityCollection = new EntityCollection();

      FullQualifiedName relatedEntityFqn = targetEntityType.getFullQualifiedName();
      String sourceEntityFqn = sourceEntity.getType();

     if (sourceEntityFqn.equals(EdmStructure.FQN_BusinessCard.getFullQualifiedNameAsString())
          && relatedEntityFqn.equals(EdmStructure.FQN_DoctypeID)) {
        int productID = (Integer) sourceEntity.getProperty("Id").getValue();

        businessCards.get(productID).getDocTypes().forEach(dt -> {
          navigationTargetEntityCollection.getEntities().add(dt);
        });

      if (navigationTargetEntityCollection.getEntities().isEmpty()) {
        return null;
      }

      return navigationTargetEntityCollection;
    }


    return null;
  }

  public static Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) {
    Entity entity = null;

    EdmEntityType edmEntityType = edmEntitySet.getEntityType();

    if (edmEntityType.getName().equals(EdmStructure.NAME_BusinessCard)) {
      entity = getBCard(edmEntityType, keyParams);
    } else if (edmEntityType.getName().equals(EdmStructure.NAME_DoctypeID)) {
      entity = getDocType(edmEntityType, keyParams);
    }

    return entity;
  }


  public static Entity getBCard(EdmEntityType edmEntityType, List<UriParameter> keyParams) {
    // the list of entities at runtime
    EntityCollection entityCollection = getBusinessCards();

    /* generic approach to find the requested entity */
    return Util.findEntity(edmEntityType, entityCollection, keyParams);
  }

  private static EntityCollection getBusinessCards() {
    EntityCollection entityCollection = new EntityCollection();
    businessCards.forEach((id, value) -> {
      entityCollection.getEntities().add(value);
    });

    return entityCollection;
  }

  private static Entity getDocType(EdmEntityType edmEntityType, List<UriParameter> keyParams) {

    // the list of entities at runtime
    EntityCollection entityCollection = getDocTypes();

    /* generic approach to find the requested entity */
    return Util.findEntity(edmEntityType, entityCollection, keyParams);
  }

  private static EntityCollection getDocTypes() {
    EntityCollection entityCollection = new EntityCollection();
    docTypeMap.forEach((id, docType) -> {
      entityCollection.getEntities().add(docType);
    });

    return entityCollection;
  }


  public static EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) {


    if (edmEntitySet.getName().equals(EdmStructure.NAME_BusinessCards)) {
      return getBusinessCards();
    }

    return null;
  }



  public static Entity getRelatedEntity(Entity entity, EdmEntityType relatedEntityType) {
    EntityCollection collection = getRelatedEntityCollection(entity, relatedEntityType);
    if (collection.getEntities().isEmpty()) {
      return null;
    }
    return collection.getEntities().get(0);
  }

  public static Entity getRelatedEntity(Entity entity, EdmEntityType relatedEntityType, List<UriParameter> keyPredicates) {
    EntityCollection relatedEntities = getRelatedEntityCollection(entity, relatedEntityType);
    return Util.findEntity(relatedEntityType, relatedEntities, keyPredicates);
  }
}
