import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

import org.apache.http.HttpHost;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.JsonpSerializable;
import org.opensearch.client.json.JsonpUtils;
import org.opensearch.client.json.jackson.JacksonJsonProvider;
import org.opensearch.client.json.jackson.JacksonJsonpGenerator;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.json.jsonb.JsonbJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.InfoResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsRequest;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.stream.JsonGenerator;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

public class Example {

    static String toJson(JsonpSerializable obj) {
        StringWriter stringWriter = new StringWriter();
        JsonbJsonpMapper mapper = new JsonbJsonpMapper();
        JsonGenerator generator = mapper.jsonProvider().createGenerator(stringWriter);
        mapper.serialize(obj, generator);
        generator.close();
        return stringWriter.toString();
    }

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
            String index = "movies";
            IndexSettings settings = new IndexSettings.Builder()
                .numberOfShards("2")
                .numberOfReplicas("1")
                .build();
            CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
                .index(index)
                .settings(settings)
                .build();

            logger.info("JSON: {}", toJson(createIndexRequest));

            try {
                client.indices().create(createIndexRequest);

                // add settings to the index
                IndexSettings indexSettings = new IndexSettings.Builder().build();
                PutIndicesSettingsRequest putSettingsRequest = new PutIndicesSettingsRequest.Builder()
                        .index(index)
                        .settings(indexSettings)
                        .build();

                logger.info("JSON: {}", toJson(putSettingsRequest));

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

            logger.info("JSON: {}", toJson(indexRequest));
            client.index(indexRequest);

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
