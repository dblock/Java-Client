# Java-Client

Makes requests to Amazon OpenSearch using the OpenSearch Java Client with native AWS SDK 2.0 transport support [added in opensarch-java 2.1.0](https://github.com/opensearch-project/opensearch-java/pull/177).

## Building

### opensearch-java

This code includes a custom-built [java-client-2.0.0-SNAPSHOT.jar](src/main/resources/java-client-2.0.0-SNAPSHOT.jar). To build it, check out [opensearch-java#main](https://github.com/opensearch-project/opensearch-java), any revision newer than [opensearch-java#177](https://github.com/opensearch-project/opensearch-java/pull/177).

```
git clone git@github.com:opensearch-project/opensearch-java.git
cd opensearch-java
```

Add shadow plugin to `build.gradle`.

```
plugins {
  id("com.github.johnrengelman.shadow") version "7.1.2"
}
```

```
apply(plugin = "com.github.johnrengelman.shadow")
```

Build shadow JAR.

```
./gradlew shadowJar
```

Copy `./java-client/build/libs/java-client-2.0.0-SNAPSHOT-all.jar` into [src/main/resources](src/main/resources).

## Running

### Install Custom JAR

Install custom-built JAR into local Maven cache.

```
mvn install:install-file -Dfile=src/main/resources/java-client-2.0.0-SNAPSHOT-all.jar -DgroupId=org.opensearch.client -DartifactId=opensearch-java -Dversion=2.1.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
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
