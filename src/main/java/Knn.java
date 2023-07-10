import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import org.apache.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.InfoResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

public class Knn {
    public static void main(final String[] args) throws IOException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(Example.class);
        SdkHttpClient httpClient = ApacheHttpClient.builder().build();

        String endpoint = System.getenv("ENDPOINT");
        if (endpoint == null) {
            throw new RuntimeException("missing ENDPOINT");
        }

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
            if (! service.equals("aoss")) {
                InfoResponse info = client.info();
                logger.info(info.version().distribution() + ": " + info.version().number());
            }

            // create the index
            String indexName = "vectors";
            CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
                .index(indexName)
                .settings(s -> s
                    .knn(true)
                )
                .mappings(m -> m
                    .properties("values", p -> p
                        .knnVector(k -> k
                        .dimension(3)
                    )
                )
            ).build();

            try {
                client.indices().create(createIndexRequest);
            } catch (OpenSearchException ex) {
                final String errorType = Objects.requireNonNull(ex.response().error().type());
                if (! errorType.equals("resource_already_exists_exception")) {
                    throw ex;
                }
            }

            JsonObject doc1 = Json.createObjectBuilder()
                .add("values", Json.createArrayBuilder().add(0.1).add(0.2).add(0.3).build())
                .add("metadata", Json.createObjectBuilder().add("genre", "drama"))
                .build();

            JsonObject doc2 = Json.createObjectBuilder()
                .add("values", Json.createArrayBuilder().add(0.2).add(0.3).add(0.4).build())
                .add("metadata", Json.createObjectBuilder().add("genre", "action"))
                .build();

            ArrayList<BulkOperation> operations = new ArrayList<>();
            operations.add(new BulkOperation.Builder().index(IndexOperation.of(io -> io.index(indexName).id("vec1").document(doc1))).build());
            operations.add(new BulkOperation.Builder().index(IndexOperation.of(io -> io.index(indexName).id("vec2").document(doc2))).build());

            // index data
            BulkRequest bulkRequest = new BulkRequest.Builder()
                .index(indexName)
                .operations(operations)
                .build();

            client.bulk(bulkRequest);
            
            // wait for the document to index
            Thread.sleep(3000);

            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexName)
                    .query(q -> q
                        .knn(k -> k
                            .field("values")
                            .vector(new float[] { 0.1f, 0.2f, 0.3f })
                            .k(2)
                        )
                    )
                .build();
            
            SearchResponse<JsonNode> searchResponse = client.search(searchRequest, JsonNode.class);
            for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
                logger.info(searchResponse.hits().hits().get(i).source().toString());
            }
            
            // delete the index
            DeleteIndexRequest deleteRequest = new DeleteIndexRequest.Builder().index(indexName).build();
            client.indices().delete(deleteRequest);
        } finally {
            httpClient.close();
        }
    }
}
