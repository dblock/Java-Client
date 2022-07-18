import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.cli.ParseException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.acm19.aws.interceptor.http.AwsRequestSigningApacheInterceptor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.regions.Region;

public class Example {

    public static void main(final String[] args) throws IOException, ParseException {
        Logger logger = LoggerFactory.getLogger(Example.class);
        CommandLineArgs opts = new CommandLineArgs("es", args);
        RestHighLevelClient client = createClient(opts.service, opts.endpoint, opts.region);

        try {
            RequestOptions.Builder requestOptions = RequestOptions.DEFAULT.toBuilder();

            // create index
            CreateIndexRequest createIndexRequest = new CreateIndexRequest("custom-index");
            createIndexRequest.settings(
                    Settings.builder()
                            .put("index.number_of_shards", 2)
                            .put("index.number_of_replicas", 1));

            // index mappings
            HashMap<String, String> typeMapping = new HashMap<String, String>();
            typeMapping.put("type", "integer");
            HashMap<String, Object> ageMapping = new HashMap<String, Object>();
            ageMapping.put("age", typeMapping);
            HashMap<String, Object> mapping = new HashMap<String, Object>();
            mapping.put("properties", ageMapping);
            createIndexRequest.mapping(mapping);

            CreateIndexResponse createIndexResponse = client.indices().create(
                    createIndexRequest, requestOptions.build());
            logger.info(createIndexResponse.toString());

            // add a document
            IndexRequest request = new IndexRequest("custom-index");
            request.id("1"); // document ID

            HashMap<String, String> stringMapping = new HashMap<String, String>();
            stringMapping.put("message:", "Testing Java REST client");
            request.source(stringMapping); // place content into the index's source

            IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
            logger.info(indexResponse.toString());

            // retrieve the document
            GetRequest getRequest = new GetRequest("custom-index", "1");
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
            logger.info(response.getSourceAsString());

            // delete the document
            DeleteRequest deleteDocumentRequest = new DeleteRequest("custom-index", "1");
            DeleteResponse deleteResponse = client.delete(deleteDocumentRequest, RequestOptions.DEFAULT);
            logger.info(deleteResponse.toString());

            // delete the index
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("custom-index");
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest,
                    RequestOptions.DEFAULT);
            logger.info(deleteIndexResponse.toString());
        } finally {
            client.close();
        }
    }

    // Adds the interceptor to the OpenSearch REST client
    public static RestHighLevelClient createClient(String service, String host, Region region) {
        HttpRequestInterceptor interceptor = new AwsRequestSigningApacheInterceptor(
                service,
                Aws4Signer.create(),
                DefaultCredentialsProvider.create(),
                region);

        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(
                RestClient.builder(HttpHost.create(host))
                        .setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor))
                        .setCompressionEnabled(true)
                        .setChunkedEnabled(false));

        return restHighLevelClient;
    }
}
