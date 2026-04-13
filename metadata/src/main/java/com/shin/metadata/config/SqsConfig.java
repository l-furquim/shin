package com.shin.metadata.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.List;

@Configuration
public class SqsConfig {

    @Bean
    SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory.<Object>builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> options.messageAttributeNames(List.of("contentType")))
                .build();
    }
}
