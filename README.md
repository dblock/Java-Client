# Java-Client

Makes requests to Amazon OpenSearch using the OpenSearch High Level REST Client. Includes a workaround to [OpenSearch#3640](https://github.com/opensearch-project/OpenSearch/issues/3640), in which a GZIP-compressed request automatically turns on chunked transfer encoding causing SigV4 requests to fail. A feature has been added in [#3884](https://github.com/opensearch-project/OpenSearch/pull/3884) to enable the user to call `.setChunkedEnabled(false)` as a workaround.

## Building

### OpenSearch 1.x

This code includes a custom-built [opensearch-rest-high-level-client-1.4.0-SNAPSHOT.jar](src/main/resources/opensearch-rest-high-level-client-1.4.0-SNAPSHOT.jar). To build it, check out [OpenSearch#1.x](https://github.com/opensearch-project/OpenSearch/tree/1.x), which had [#3884](https://github.com/opensearch-project/OpenSearch/pull/3884) merged.

```
git clone git@github.com:opensearch-project/OpenSearch.git
cd OpenSearch
git checkout 1.x
./gradlew :client:rest-high-level:shadowJar
```

Copy `./client/rest-high-level/build/distributions/opensearch-rest-high-level-client-1.4.0-SNAPSHOT.jar` into [src/main/resources](src/main/resources).

## Running

### Install Custom JAR

Install custom-built JAR into local Maven cache.

```
mvn install:install-file -Dfile=src/main/resources/opensearch-rest-high-level-client-1.4.0-SNAPSHOT.jar -DgroupId=org.opensearch.client -DartifactId=opensearch-rest-high-level-client -Dversion=1.4.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
```

### Build and Run Sample

Create an OpenSearch domain in (AWS) which support IAM based AuthN/AuthZ.

```
export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=
export AWS_SESSION_TOKEN=

export OPENSEARCH_ENDPOINT=https://....us-west-2.es.amazonaws.com
export OPENSEARCH_REGION=us-west-2

mvn install
mvn compile exec:java \
  -Dexec.mainClass="Example" \
  -Dexec.args="--endpoint=$OPENSEARCH_ENDPOINT --region=$OPENSEARCH_REGION" \
  -Dlog4j.configurationFile=target/log4j2.xml \
  -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog \
  -Dorg.apache.commons.logging.simplelog.log.org.apache.http.wire=DEBUG
```

The code will create an index, add a document, then cleanup.

## License 

This project is licensed under the [Apache v2.0 License](LICENSE.txt).

## Copyright

Copyright OpenSearch Contributors. See [NOTICE](NOTICE.txt) for details.
