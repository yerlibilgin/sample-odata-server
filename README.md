# Sample-OData-Server

this server works as proxy to the existing
TOOP directory and reinterprets its results to 
and ODATA model and ODATA results.

### Running

```shell script
mvn jetty:run
```

### Sample Curl Commands

#### Get Metadata
```shell script

curl --request GET \
  --url http://localhost:8080/dsd.svc/%24metadata \
  --header 'accept: application/json'

```
  

#### Get Service Document
  
```shell script

curl --request GET \
  --url http://localhost:8080/dsd.svc \
  --header 'accept: application/json'

```

#### All Business Cards

```shell script

curl --request GET \
  --url http://localhost:8080/dsd.svc/BusinessCards \
  --header 'accept: application/json'

```

#### BusinessCards(0)

```shell script

curl --request GET \
  --url 'http://localhost:8080/dsd.svc/BusinessCards(0)' \
  --header 'accept: application/json'

```

#### BusinessCards(0) Expand Doctypes

```shell script

curl --request GET \
  --url 'http://localhost:8080/dsd.svc/BusinessCards(0)?%24expand=DoctypeIDs' \
  --header 'accept: application/json'

```


#### BusinessDoc-Docid-expand-select value
```shell script

curl --request GET \
  --url 'http://localhost:8080/dsd.svc/BusinessCards?%24expand=DoctypeIDs(%24select%3Dscheme)'

```


#### DocTypeQuery1

```shell script

curl --request GET \
  --url 'http://localhost:8080/dsd.svc/BusinessCards?%24expand=DoctypeIDs(%24filter%3DId%20eq%209)' \
  --header 'accept: application/json'

```

#### DocTypeQuery for a country and doctype

```shell script

curl --request GET \
  --url 'http://localhost:8080/dsd.svc/BusinessCards?%24expand=DoctypeIDs(%24filter%3Dvalue%20eq%20'\''urn%3Aeu%3Atoop%3Ans%3Adataexchange-1p40%3A%3AResponse%23%23urn%3Aeu.toop.response.crewcertificate-list%3A%3A1.40'\'')&%24filter=contains(Entity%2Fcountrycode%2C%20'\''EE'\'')' \
  --header 'accept: application/json'

  ```
  
  In this query, first the country code is filtered,  then the doctypes are expanded and filtered. 

##### Sample json output of the last query
```json

{
  "@odata.context": "$metadata#BusinessCards(DoctypeIDs())",
  "value": [
    {
      "Id": 16,
      "Participant": {
        "scheme": "iso6523-actorid-upis",
        "value": "iso6523-actorid-upis"
      },
      "Entity": {
        "countrycode": "EE",
        "name": "Estonian Maritime Administration"
      },
      "DoctypeIDs": [
        {
          "Id": 67,
          "scheme": "toop-doctypeid-qns",
          "value": "urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.crewcertificate-list::1.40"
        }
      ]
    }
  ]
}

```
##### Sample xml output of the last query

Same query as xml:

```shell script

curl --request GET \
  --url 'http://localhost:8080/dsd.svc/BusinessCards?%24expand=DoctypeIDs(%24filter%3Dvalue%20eq%20'\''urn%3Aeu%3Atoop%3Ans%3Adataexchange-1p40%3A%3AResponse%23%23urn%3Aeu.toop.response.crewcertificate-list%3A%3A1.40'\'')&%24filter=contains(Entity%2Fcountrycode%2C%20'\''EE'\'')' \
  --header 'accept: application/xml'
```

Output:

```xml
<?xml version='1.0' encoding='UTF-8'?>
<a:feed
  xmlns:a="http://www.w3.org/2005/Atom"
  xmlns:m="http://docs.oasis-open.org/odata/ns/metadata"
  xmlns:d="http://docs.oasis-open.org/odata/ns/data" m:context="$metadata#BusinessCards(DoctypeIDs())">
  <a:id>http://localhost:8080/dsd.svc/BusinessCards</a:id>
  <a:entry>
    <a:id>BusinessCards(Id)</a:id>
    <a:title/>
    <a:summary/>
    <a:updated>2019-12-18T23:37:29Z</a:updated>
    <a:author>
      <a:name/>
    </a:author>
    <a:link rel="edit" href="BusinessCards(Id)"/>
    <a:link rel="http://docs.oasis-open.org/odata/ns/relatedlinks/DoctypeIDs" type="application/atom+xml;type=entry" title="DoctypeIDs">
      <m:inline>
        <a:feed>
          <a:entry>
            <a:id>BusinessCards(Id)</a:id>
            <a:title/>
            <a:summary/>
            <a:updated>2019-12-18T23:37:29Z</a:updated>
            <a:author>
              <a:name/>
            </a:author>
            <a:link rel="edit" href="BusinessCards(Id)"/>
            <a:category scheme="http://docs.oasis-open.org/odata/ns/scheme" term="#eu.toop.dsd.odata.DoctypeID"/>
            <a:content type="application/xml">
              <m:properties>
                <d:Id m:type="Int32">67</d:Id>
                <d:scheme>toop-doctypeid-qns</d:scheme>
                <d:value>urn:eu:toop:ns:dataexchange-1p40::Response##urn:eu.toop.response.crewcertificate-list::1.40</d:value>
              </m:properties>
            </a:content>
          </a:entry>
        </a:feed>
      </m:inline>
    </a:link>
    <a:category scheme="http://docs.oasis-open.org/odata/ns/scheme" term="#eu.toop.dsd.odata.BusinessCard"/>
    <a:content type="application/xml">
      <m:properties>
        <d:Id m:type="Int32">16</d:Id>
        <d:Participant m:type="#eu.toop.dsd.odata.Participant">
          <d:scheme>iso6523-actorid-upis</d:scheme>
          <d:value>iso6523-actorid-upis</d:value>
        </d:Participant>
        <d:Entity m:type="#eu.toop.dsd.odata.Entity">
          <d:countrycode>EE</d:countrycode>
          <d:name>Estonian Maritime Administration</d:name>
        </d:Entity>
      </m:properties>
    </a:content>
  </a:entry>
</a:feed>
```

__Note__: The query for the DocType that is listed under a business card has to be executed within an expand, 
the reason is that a doctype is a navigation property for a business card, therefore it is lazy loaded, first you have
to expand it and then you have to filter it.

The country code query first filters the business card with respect to the country,
then the doctype query filters the expanded doctype with respect to the value.

The filter by both scheme and value is possible but needs more boilerplate. I don't think that is currently needed.

__Note2__: The current code mimics a catalog, and doctypes/participant ids listed under business cards. The current toop-directory does not provide
such a query. SInce I wanted to explore the capabilities of Olingo, I implemented that kind of comlicated model and queries. If OData will be adopted,
a shorter and better model should be created so that the
queries are simpler and easier to implement and the boilerplate doesn't drown us.
