import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.common.settings.Settings;

import io.github.acm19.aws.interceptor.http.AwsRequestSigningApacheInterceptor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.regions.Region;

public class RESTClientTest {

    private static String host = "https://search-dblock-test-opensearch-21-tu5gqrjd4vg4qazjsu6bps5zsy.us-west-2.es.amazonaws.com"; // put
                                                                                                                                   // your
                                                                                                                                   // own
                                                                                                                                   // end-point
    private static String service = "es";
    private static Region region = Region.US_WEST_2;

    public static void main(String[] args) throws IOException {
        RestHighLevelClient searchClient = searchClient(service, region);

        try {
            RequestOptions.Builder requestOptions = RequestOptions.DEFAULT.toBuilder();
            // CASE 1: create index
            CreateIndexRequest createIndexRequest = new CreateIndexRequest("custom-index");

            createIndexRequest.settings(Settings.builder() // Specify in the settings how many shards you want in the
                                                           // index.
                    .put("index.number_of_shards", 2)
                    .put("index.number_of_replicas", 1));
            // Create a set of maps for the index's mappings.
            HashMap<String, String> typeMapping = new HashMap<String, String>();
            typeMapping.put("type", "integer");
            HashMap<String, Object> ageMapping = new HashMap<String, Object>();
            ageMapping.put("age", typeMapping);
            HashMap<String, Object> mapping = new HashMap<String, Object>();
            mapping.put("properties", ageMapping);
            createIndexRequest.mapping(mapping);

            CreateIndexResponse createIndexResponse = searchClient.indices().create(createIndexRequest,
                    requestOptions.build());
            System.out.println(createIndexResponse.toString());

            // CASE 2: Adding data to the index.
            IndexRequest request = new IndexRequest("custom-index"); // Add a document to the custom-index we created.
            request.id("1"); // Assign an ID to the document.

            HashMap<String, String> stringMapping = new HashMap<String, String>();
            stringMapping.put("message:", "Testing Java REST client");
            request.source(stringMapping); // Place your content into the index's source.

            IndexResponse indexResponse = searchClient.index(request, RequestOptions.DEFAULT);
            System.out.println(indexResponse.toString());

            // CASE 3: Getting back the document

            GetRequest getRequest = new GetRequest("custom-index", "1");

            GetResponse response = searchClient.get(getRequest, RequestOptions.DEFAULT);
            System.out.println(response.getSourceAsString());

            // CASE 4: Delete the document
            DeleteRequest deleteDocumentRequest = new DeleteRequest("custom-index", "1"); // Index name followed by the
                                                                                          // ID.

            DeleteResponse deleteResponse = searchClient.delete(deleteDocumentRequest, RequestOptions.DEFAULT);
            System.out.println(deleteResponse.toString());

            // CASE 5: Delete the index
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("custom-index"); // Index name.

            AcknowledgedResponse deleteIndexResponse = searchClient.indices().delete(deleteIndexRequest,
                    RequestOptions.DEFAULT);
            System.out.println(deleteIndexResponse.toString());

            searchClient.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            searchClient.close();
        }
    }

    // Adds the interceptor to the OpenSearch REST client
    public static RestHighLevelClient searchClient(String service, Region region) {
        HttpRequestInterceptor interceptor = new AwsRequestSigningApacheInterceptor(
                service,
                Aws4Signer.create(),
                DefaultCredentialsProvider.create(),
                region);

        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(
                RestClient.builder(HttpHost.create(host))
                        .setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor))
                        .setCompressionEnabled(false));

        return restHighLevelClient;
    }
}
