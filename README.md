# OpenSearch Java Client Demo

Makes requests to Amazon OpenSearch using the OpenSearch Java Client with native AWS SDK 2.0 transport support [added in opensarch-java 2.1.0](https://github.com/opensearch-project/opensearch-java/pull/177). 

For support with older versions and for using OpenSearch High Level REST Client directly, see [opensearch-1.x](https://github.com/dblock/opensearch-java-client-demo/tree/opensearch-1.x) or [opensearch-2.x](https://github.com/dblock/opensearch-java-client-demo/tree/opensearch-2.x).

## Running

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
