# Java-Client

Workaround to https://github.com/opensearch-project/OpenSearch/issues/3640 of setting `.setChunkedEnabled(false)`.

1. Optionally, build https://github.com/opensearch-project/OpenSearch/pull/3884.

```
git clone git@github.com:opensearch-project/OpenSearch.git
cd OpenSearch
git checkout backport/backport-3864-to-1.x
./gradlew :client:rest-high-level:shadowJar
```

This produces `./client/rest-high-level/build/distributions/opensearch-rest-high-level-client-1.4.0-SNAPSHOT.jar`.

2. Install custom-built JAR.

```
mvn install:install-file -Dfile=src/main/resources/opensearch-rest-high-level-client-1.4.0-SNAPSHOT.jar -DgroupId=org.opensearch.client -DartifactId=opensearch-rest-high-level-client -Dversion=1.4.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
```

3. Create openSearch domain in (AWS) which support IAM based AuthN/AuthZ.

Update the value of `host` and `region` in [RESTClientTest.java](/src/main/java/RESTClientTest.java#L27) to your endpoint.

```
export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=
export AWS_SESSION_TOKEN=

mvn install
mvn compile exec:java -Dexec.mainClass="RESTClientTest"
```

The code will create an index, add a document, then cleanup.
