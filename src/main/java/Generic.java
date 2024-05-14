import java.io.IOException;
import org.apache.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.Bodies;
import org.opensearch.client.opensearch.generic.OpenSearchClientException;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient.ClientOptions;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

public class Generic {

    public static void main(final String[] args) throws IOException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(Example.class);

        String endpoint = System.getenv("ENDPOINT");
        if (endpoint == null) {
            throw new RuntimeException("missing ENDPOINT");
        }

        SdkHttpClient httpClient = ApacheHttpClient.builder().build();
        String region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        String service = System.getenv().getOrDefault("SERVICE", "es");

        try {
            OpenSearchClient client = new OpenSearchClient(
                    new AwsSdk2Transport(
                            httpClient,
                            HttpHost.create(endpoint).getHostName(),
                            service,
                            Region.of(region),
                            AwsSdk2TransportOptions.builder().build()));

            // TODO: remove when Serverless supports GET /
            if (!service.equals("aoss")) {
                Response response = client.generic().execute(
                        Requests.builder()
                                .endpoint("/")
                                .method("GET")
                                .build());

                logger.info(response.getBody().get().bodyAsString());
            }

            // create the index
            String index = "movies";

            OpenSearchGenericClient genericClient = client.generic()
                    .withClientOptions(ClientOptions.throwOnHttpErrors());

            try {
                logger.info(genericClient.execute(
                        Requests.builder()
                                .endpoint(index)
                                .method("PUT")
                                .json("{}")
                                .build())
                        .getBody().get().bodyAsString());
            } catch (OpenSearchClientException ex) {
                JsonNode json = ex.response().getBody()
                        .map(b -> Bodies.json(b, JsonNode.class, client._transport().jsonpMapper()))
                        .orElse(null);
                String errorType = json.get("error").get("type").textValue();
                if (!errorType.equals("resource_already_exists_exception")) {
                    System.err.println(ex.response().getBody().get().bodyAsString());
                    throw ex;
                }
            }

            // index data
            logger.info(genericClient.execute(
                    Requests.builder()
                            .endpoint(index + "/_doc/1")
                            .method("POST")
                            .json("{\"director\":\"Bennett Miller\",\"title\":\"Moneyball\",\"year\":2011}")
                            .build())
                    .getBody().get().bodyAsString());

            // update data
            logger.info(genericClient.execute(
                    Requests.builder()
                            .endpoint(index + "/_doc/1")
                            .method("PUT")
                            .json("{\"director\":\"Bennett Miller\",\"title\":\"Moneyball 2\",\"year\":2011}")
                            .build())
                    .getBody().get().bodyAsString());

            // wait for the document to index
            Thread.sleep(3000);

            Response searchResponse = genericClient.execute(
                Requests.builder().endpoint(index + "/_search").method("POST")
                        .json("{"
                              + " \"query\": {"
                              + "  \"match\": {"
                              + "    \"title\": {"
                              + "      \"query\": \"Moneyball 2\""
                              + "    }"
                              + "  }"
                              + " }"
                              + "}")
                .build());

            JsonNode json = searchResponse.getBody()
                .map(b -> Bodies.json(b, JsonNode.class, client._transport().jsonpMapper()))
                .orElse(null);
            
            JsonNode hits = json.get("hits").get("hits");
            for (int i = 0; i < hits.size(); i++) {
                logger.info(hits.get(i).get("_source").toString());
            }

            // delete the document
            logger.info(genericClient.execute(
                    Requests.builder()
                            .endpoint(index + "/_doc/1")
                            .method("DELETE")
                            .build())
                    .getBody().get().bodyAsString());

            // delete the index
            logger.info(genericClient.execute(
                    Requests.builder()
                            .endpoint(index)
                            .method("DELETE")
                            .build())
                    .getBody().get().bodyAsString());

        } finally {
            httpClient.close();
        }
    }
}
