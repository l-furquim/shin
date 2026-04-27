package com.shin.search.config;

import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

@Configuration
public class OpenSearchConfig {

    @Value("${spring.cloud.aws.collection-endpoint}")
    private String collectionEndpoint;

    @Value("${spring.cloud.aws.region:us-east-1}")
    private String region;

    @Bean(destroyMethod = "close")
    public SdkHttpClient sdkHttpClient() {
        return ApacheHttpClient.builder().build();
    }

    @Bean
    public OpenSearchClient openSearchClient(SdkHttpClient sdkHttpClient) {
        OpenSearchTransport transport = new AwsSdk2Transport(
                sdkHttpClient,
                collectionEndpoint.replace("https://", ""),
                "aoss",
                Region.of(region),
                AwsSdk2TransportOptions.builder()
                        .setMapper(new JacksonJsonpMapper())
                        .build()
        );
        return new OpenSearchClient(transport);
    }
}
