import java.util.Objects;

import org.apache.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.UpdateResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsRequest;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

public class LoggingClientExample {

    public static void main(final String[] args) throws Exception {
        Logger logger = LoggerFactory.getLogger(LoggingClientExample.class);
        SdkHttpClient httpClient = ApacheHttpClient.builder().build();

        String endpoint = System.getenv("ENDPOINT");
        if (endpoint == null) {
            throw new RuntimeException("missing ENDPOINT");
        }

        String region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        String service = System.getenv().getOrDefault("SERVICE", "es");

        try {
            AwsSdk2Transport transport = new AwsSdk2Transport(
                httpClient,
                HttpHost.create(endpoint).getHostName(),
                service,
                Region.of(region),
                AwsSdk2TransportOptions.builder().build());

            OpenSearchClient client = OpenSearchClientProxy.create(transport);

            // create the index
            String index = "movies";
            CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder().index(index).build();

            try {
                client.indices().create(createIndexRequest);

                // add settings to the index
                IndexSettings indexSettings = new IndexSettings.Builder().build();
                PutIndicesSettingsRequest putSettingsRequest = new PutIndicesSettingsRequest.Builder()
                    .index(index)
                    .settings(indexSettings)
                    .build();
                client.indices().putSettings(putSettingsRequest);
            } catch (OpenSearchException ex) {
                final String errorType = Objects.requireNonNull(ex.response().error().type());
                if (! errorType.equals("resource_already_exists_exception")) {
                    throw ex;
                }
            }

            // index data
            Movie movie = new Movie("Bennett Miller", "Moneyball", 2011);
            IndexRequest<Movie> indexRequest = new IndexRequest.Builder<Movie>()
                .index(index)
                .id("1")
                .document(movie)
                .build();
            IndexResponse indexResponse = client.index(indexRequest);
            System.out.println(String.format("Document %s.", indexResponse.result().toString().toLowerCase()));

            // update data
            Movie movieUpdate = new Movie("Bennett Miller", "Moneyball 2", 2011);
            UpdateRequest<Movie, Movie> updateRequest = new UpdateRequest.Builder<Movie, Movie>()
                .id("1")
                .index(index)
                .doc(movieUpdate)
                .build();
            UpdateResponse<Movie> updateResponse = client.update(updateRequest, Movie.class);
            System.out.println(String.format("Document %s.", updateResponse.result().toString().toLowerCase()));

            // wait for the document to index
            Thread.sleep(3000);

            // search for the document
            SearchResponse<Movie> searchResponse = client.search(s -> s.index(index), Movie.class);
            for (int i = 0; i < searchResponse.hits().hits().size(); i++) {
                logger.info(searchResponse.hits().hits().get(i).source().toString());
            }

            // delete the document
            client.delete(b -> b.index(index).id("1"));

            // delete the index
            DeleteIndexRequest deleteRequest = new DeleteIndexRequest.Builder().index(index).build();
            client.indices().delete(deleteRequest);
        } finally {
            httpClient.close();
        }
    }
}
