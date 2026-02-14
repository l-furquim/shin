package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.EncodeFinishedEvent;
import com.shin.metadata.service.VideoService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EncodeFinishedConsumer {

    private final ObjectMapper objectMapper;
    private final VideoService videoService;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.encode-finished-queue}")
    public void consume(
            String message
    ) {
        try {
            log.info("Received message from encode-finished-queue={}", message);

            JsonNode node = objectMapper.readTree(message);

            String messageJson = node.get("Message").asText();

            EncodeFinishedEvent event = objectMapper.readValue(messageJson, EncodeFinishedEvent.class);

            log.info("Received encode finished message={}", event);

            videoService.updateVideoProcessingStatus(
                    event.videoId(),
                    event.status(),
                    event.resolutions(),
                    event.duration()
            );

        } catch (Exception e) {
            log.error("Error while consuming encode finished event: ", e);
        }
    }

}
