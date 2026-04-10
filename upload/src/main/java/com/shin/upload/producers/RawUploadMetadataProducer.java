package com.shin.upload.producers;

import com.shin.upload.dto.RawUploadCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class RawUploadMetadataProducer {

    private final SqsTemplate template;
    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.queues.raw-upload-metadata-queue}")
    private String queue;

    public void send(RawUploadCreatedEvent event) {
        try {
            template.send(queue, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to send event to metadata queue", e);
        }
    }
}
