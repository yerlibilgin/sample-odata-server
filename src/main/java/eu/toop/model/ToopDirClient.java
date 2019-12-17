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
import org.apache.http.client.methods.HttpGet;

import eu.peppol.schema.pd.businesscard_generic._201907.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

public class ToopDirClient {
  private static final int MAX_RESULTS_PER_PAGE = 100;

  private static final String m_sBaseURL = "http://directory.acc.exchange.toop.eu";
  private static final String TOOP_DIR_EXPORT_URL = "http://directory.acc.exchange.toop.eu/export/businesscards";

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

  private static RootType rootType;

  public static ICommonsSet<IParticipantIdentifier> getAllParticipantIDs() {
    //ObjectFactory of = new ObjectFactory();
    //.

    //return null;

    if (rootType == null) {
      try {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        final JAXBElement<RootType> unmarshal = (JAXBElement<RootType>) jaxbUnmarshaller.unmarshal(new URL(TOOP_DIR_EXPORT_URL));
        rootType = unmarshal.getValue();
      } catch (Exception ex) {
        throw new IllegalStateException("Sorry Cannot read directory.", ex);
      }
    }

    CommonsHashSet<IParticipantIdentifier> set = new CommonsHashSet<>();
    rootType.getBusinesscard().forEach(bc -> {

      set.add(new SimpleParticipantIdentifier(bc.getParticipant().getScheme(),
          bc.getParticipant().getValue()));

    });

    return set;
  }

  @Override
  public String toString() {
    return new ToStringGenerator(this).append("BaseURL", m_sBaseURL).getToString();
  }
}
