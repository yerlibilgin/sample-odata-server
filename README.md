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

#### DocTypeQuery2

```shell script

curl --request GET \
  --url 'http://localhost:8080/dsd.svc/BusinessCards?%24expand=DoctypeIDs&%24filter=contains(Participant%2Fvalue%2C%20'\''ishipcertificate'\'')' \
  --header 'accept: application/json'

  ```
  
