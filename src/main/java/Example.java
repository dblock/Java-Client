import io.github.acm19.aws.interceptor.http.AwsRequestSigningApacheV5Interceptor;
import org.apache.commons.cli.ParseException;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.IntegerNumberProperty;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.hc.core5.http2.HttpVersionPolicy.FORCE_HTTP_1;

public class Example {

    public static void main(final String[] args) throws IOException, ParseException, URISyntaxException {
        Logger logger = LoggerFactory.getLogger(Example.class);
        CommandLineArgs opts = new CommandLineArgs("es", args);
        OpenSearchTransport openSearchTransport = createTransport(opts.service, opts.endpoint, opts.region);
        OpenSearchClient client = new OpenSearchClient(openSearchTransport);
        OpenSearchClusterClient clusterClient = new OpenSearchClusterClient(openSearchTransport);

        String endpoint = System.getenv("ENDPOINT");
        if (endpoint == null) {
            throw new RuntimeException("missing ENDPOINT");
        }

        String region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        String service = System.getenv().getOrDefault("SERVICE", "es");
        
        try {
            // check cluster health (GET request)
            HealthResponse health = clusterClient.health();

            logger.info("cluster {} status is {}.", health.clusterName(), health.status());

            // create index
            CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
                    .index("custom-index")
                    .settings(
                            new IndexSettings.Builder()
                                    .numberOfShards("2")
                                    .numberOfReplicas("1")
                                    .build()
                    )
                    .mappings(new TypeMapping.Builder()
                            .properties("age", new Property(new IntegerNumberProperty.Builder().build()))
                            .build()
                    )
                    .build();
            CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest);

            logger.info(createIndexResponse.toString());

            // add a document
        } finally {
            client._transport().close();
        }
    }

    public static OpenSearchTransport createTransport(String service, String host, Region region) throws URISyntaxException {
        return ApacheHttpClient5TransportBuilder.builder(HttpHost.create(new URI(host)))
                .setHttpClientConfigCallback(clientBuilder -> clientBuilder
                        .addRequestInterceptorLast(new AwsRequestSigningApacheV5Interceptor(service, Aws4Signer.create(), DefaultCredentialsProvider.create(), region))
                        .setConnectionManager(PoolingAsyncClientConnectionManagerBuilder.create()
                                .setDefaultTlsConfig(TlsConfig.custom().setVersionPolicy(FORCE_HTTP_1).build())
                                .build())
                )
                .build();
    }
}
