# Sample-OData-Server

this server works as proxy to the existing
TOOP directory and reinterprets its results to 
and ODATA model and ODATA results.

### Running

```bash
mvn jetty:run
```

### Sample Curl Commands

#### Get Metadata
```
curl --request GET \
  --url http://localhost:8080/dsd.svc/%24metadata \
  --header 'accept: application/json'
  ```
  
#### Get Service Document
  
```
curl --request GET \
  --url http://localhost:8080/dsd.svc \
  --header 'accept: application/json'
```

#### List ParticipantIdentifiers

```
curl --request GET \
  --url http://localhost:8080/dsd.svc/ParticipantIdentifiers \
  --header 'accept: application/json'
  ```
  
