import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.apache.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
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

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

public class Example {

    public static void main(final String[] args) throws IOException, ParseException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(Example.class);
        CommandLineArgs opts = new CommandLineArgs(args);
        SdkHttpClient httpClient = ApacheHttpClient.builder().build();

        try {
            OpenSearchClient client = new OpenSearchClient(
                    new AwsSdk2Transport(
                            httpClient,
                            HttpHost.create(opts.endpoint).getHostName(),
                            opts.region,
                            AwsSdk2TransportOptions.builder().build()));

            InfoResponse info = client.info();
            logger.info(info.version().distribution() + ": " + info.version().number());

            // create the index
            String index = "sample-index";
            CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder().index(index).build();
            client.indices().create(createIndexRequest);

            // add settings to the index
            IndexSettings indexSettings = new IndexSettings.Builder().autoExpandReplicas("0-all").build();
            PutIndicesSettingsRequest putSettingsRequest = new PutIndicesSettingsRequest.Builder()
                    .index(index)
                    .settings(indexSettings)
                    .build();
            client.indices().putSettings(putSettingsRequest);

            // index data
            IndexData indexData = new IndexData("first_name", "Bruce");
            IndexRequest<IndexData> indexRequest = new IndexRequest.Builder<IndexData>()
                    .index(index)
                    .id("1")
                    .document(indexData)
                    .build();
            client.index(indexRequest);

            // wait for the document to index
            Thread.sleep(3000);

            // search for the document
            SearchResponse<IndexData> searchResponse = client.search(s -> s.index(index), IndexData.class);
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
