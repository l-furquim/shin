package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.EncodingProgressEvent;
import com.shin.metadata.service.VideoProcessingService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Slf4j
@Component
public class EncodingProgressConsumer {

    private final ObjectMapper objectMapper;

    private final VideoProcessingService videoProcessingService;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.encoding-progress-queue}")
    public void consume(
          String message
    ) {
        try {
            EncodingProgressEvent event = resolveEvent(message);

            if (event == null) {
                return;
            }

            this.videoProcessingService.update(
                    event.videoId(),
                    null,
                    null,
                    null,
                    null,
                    event.progress(),
                    event.failure()
            );

            log.info("Updated video={}, processing status={}", event.videoId(), event.progress());
        } catch (Exception e) {
            log.error("Error while consuming encoding progress event", e);
        }
    }

    private EncodingProgressEvent resolveEvent(String message) throws Exception {
        JsonNode node = objectMapper.readTree(message);

        if (node.has("Message") && node.get("Message").isTextual()) {
            return objectMapper.readValue(node.get("Message").asText(), EncodingProgressEvent.class);
        }

        return objectMapper.readValue(message, EncodingProgressEvent.class);
    }

}
