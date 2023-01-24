# OpenSearch Java Client Demo

Makes requests to Amazon OpenSearch using the OpenSearch Java Client with native AWS SDK 2.0 transport support [added in opensarch-java 2.1.0](https://github.com/opensearch-project/opensearch-java/pull/177). Supports OpenSearch Serverless since [opensearch-java 2.2.0](https://github.com/opensearch-project/opensearch-java/pull/339). 

For support with older versions and for using OpenSearch High Level REST Client directly, see [opensearch-1.x](https://github.com/dblock/opensearch-java-client-demo/tree/opensearch-1.x) or [opensearch-2.x](https://github.com/dblock/opensearch-java-client-demo/tree/opensearch-2.x).

## Running

Create an OpenSearch domain in (AWS) which support IAM based AuthN/AuthZ.

```
export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=
export AWS_SESSION_TOKEN=
export AWS_REGION=us-west-2

export SERVICE=es # use "aoss" for OpenSearch Serverless
export ENDPOINT=https://....us-west-2.es.amazonaws.com

mvn install
mvn compile exec:java \
  -Dexec.mainClass="Example" \
  -Dlog4j.configurationFile=target/log4j2.xml \
  -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog \
  -Dorg.apache.commons.logging.simplelog.log.org.apache.http.wire=INFO
```

The [code](src/main/java/Example.java) will show the server version, create an index, add a document, search for it, output the result, then cleanup.

```
2022-12-26 15:55:02 [Example.main()] INFO  - opensearch: 2.3.0
2022-12-26 15:55:04 [Example.main()] INFO  - Movie{Director='Bennett Miller', Title='Moneyball', Year=2011}
```

## License 

This project is licensed under the [Apache v2.0 License](LICENSE.txt).

## Copyright

Copyright OpenSearch Contributors. See [NOTICE](NOTICE.txt) for details.
