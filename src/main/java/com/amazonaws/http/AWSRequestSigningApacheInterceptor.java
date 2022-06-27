/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.http;

import static org.apache.http.protocol.HttpCoreContext.HTTP_TARGET_HOST;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

/**
 * An {@link HttpRequestInterceptor} that signs requests using any AWS {@link Signer}
 * and {@link AWSCredentialsProvider}.
 */
public class AWSRequestSigningApacheInterceptor implements HttpRequestInterceptor {
    /**
     * The service that we're connecting to. Technically not necessary.
     * Could be used by a future Signer, though.
     */
    private final String service;

    private final Region region;

    /**
     * The particular signer implementation.
     */
    private final Aws4Signer signer;

    /**
     * The source of AWS credentials for signing.
     */
    private final AwsCredentialsProvider awsCredentialsProvider;

    /**
     * @param service                service that we're connecting to
     * @param region                 region
     */
    public AWSRequestSigningApacheInterceptor(
        final String service,
        final Region region) {
        this.service = service;
        this.region = region;

        this.signer = Aws4Signer.create();
        this.awsCredentialsProvider = DefaultCredentialsProvider.create();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {

        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(request.getRequestLine().getUri());
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI", e);
        }

        HttpHost host = (HttpHost) context.getAttribute(HTTP_TARGET_HOST);

        System.out.println("\n" + request.getRequestLine().getMethod() + " " + host.toString() + uriBuilder.toString());

        SdkHttpFullRequest.Builder httpRequestBuilder = SdkHttpFullRequest.builder().protocol(host.getSchemeName());
        httpRequestBuilder = httpRequestBuilder.host(host.getHostName());
        httpRequestBuilder = httpRequestBuilder.port(host.getPort());
        httpRequestBuilder = httpRequestBuilder.method(SdkHttpMethod.fromValue(request.getRequestLine().getMethod()));
        httpRequestBuilder = httpRequestBuilder.encodedPath(uriBuilder.getPath());
        httpRequestBuilder = httpRequestBuilder.rawQueryParameters(nvpToMapParams(uriBuilder.getQueryParams()));

        long contentLength = -1L;
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) request;
            if (httpEntityEnclosingRequest.getEntity() != null) {
                Header contentEncodingHeader = httpEntityEnclosingRequest.getFirstHeader("Content-Encoding");
                if (contentEncodingHeader != null && contentEncodingHeader.getValue() == "gzip") {
                    GzipDecompressingEntity decompressedEntity = new GzipDecompressingEntity(httpEntityEnclosingRequest.getEntity());
                    String decompressedData = EntityUtils.toString(decompressedEntity);
                    System.out.println(decompressedData);
                    byte[] content = decompressedData.getBytes();
                    contentLength = content.length;
                    ContentStreamProvider syncBody = () -> new ByteArrayInputStream(content);
                    httpRequestBuilder = httpRequestBuilder.contentStreamProvider(syncBody);
                } else {
                    InputStream dataStream = httpEntityEnclosingRequest.getEntity().getContent();
                    byte[] content = dataStream.readAllBytes();
                    System.out.println(new String(content));
                    ContentStreamProvider syncBody = () -> new ByteArrayInputStream(content);
                    httpRequestBuilder = httpRequestBuilder.contentStreamProvider(syncBody);
                }
            }
        }
        
        List<Header> headers = new ArrayList<>();
        headers.addAll(Arrays.asList(request.getAllHeaders()));

        if (contentLength > 0) {
            headers.add(new BasicHeader("x-Amz-Decoded-Content-Length", Long.toString(contentLength)));
        }

        // Sign it
        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(this.awsCredentialsProvider.resolveCredentials())
                .signingName(this.service)
                .signingRegion(this.region)
                .build();

        httpRequestBuilder = httpRequestBuilder.headers(headerArrayToMap(headers));
        
        SdkHttpFullRequest signableRequest = httpRequestBuilder.build();
        
        signableRequest = signer.sign(signableRequest, signerParams);

        // Now copy everything back
        request.setHeaders(mapToHeaderArray(signableRequest.headers()));

        if (request instanceof HttpEntityEnclosingRequest) { 
            HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) request;
            if (httpEntityEnclosingRequest.getEntity() != null) {
                // BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
                // basicHttpEntity.setContent(signableRequest.contentStreamProvider());
                // httpEntityEnclosingRequest.setEntity(basicHttpEntity);
            }
        }

        for(Header header : request.getAllHeaders()) {
            System.out.println(header.getName() + ": " + header.getValue());
        }
    }

    /**
     * @param headers modeled Header objects
     * @return a Map of header entries
     */
    private static Map<String, List<String>> headerArrayToMap(List<Header> headers) {
        Map<String, List<String>> headersMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Header header : headers) {
            List<String> values = new ArrayList<String>();
            values.add(header.getValue());
            headersMap.put(header.getName(), values);
        }
        return headersMap;
    }

    /**
     * @param params list of HTTP query params as NameValuePairs
     * @return a multimap of HTTP query params
     */
    private static Map<String, List<String>> nvpToMapParams(final List<NameValuePair> params) {
        Map<String, List<String>> parameterMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (NameValuePair nvp : params) {
            List<String> argsList =
                    parameterMap.computeIfAbsent(nvp.getName(), k -> new ArrayList<>());
            argsList.add(nvp.getValue());
        }
        return parameterMap;
    }

    /**
     * @param mapHeaders Map of header entries
     * @return modeled Header objects
     */
    private static Header[] mapToHeaderArray(final Map<String, List<String>> mapHeaders) {
        List<Header> headers = new ArrayList<Header>();
        for (Map.Entry<String, List<String>> headerEntry : mapHeaders.entrySet()) {
            for(String headerValue : headerEntry.getValue()) {
                headers.add(new BasicHeader(headerEntry.getKey(), headerValue));
            }
        }
        return headers.toArray(new Header[0]);
    }
}
