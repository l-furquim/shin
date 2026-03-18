package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.IncreaseViewCountEvent;
import com.shin.metadata.service.VideoService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class IncreaseViewCountConsumer {

    private final ObjectMapper objectMapper;
    private final VideoService videoService;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.encode-finished-queue}")
    public void consume(String message) {
        try {
            log.info("Received message from encode-finished-queue={}", message);

            JsonNode node = objectMapper.readTree(message);

            String messageJson = node.get("Message").asText();

            IncreaseViewCountEvent event = objectMapper.readValue(messageJson, IncreaseViewCountEvent.class);

            log.info("Received encode finished message={}", event);

            videoService.increaseVideoView(
                    event.videoId(),
                    event.userId()
            );

        } catch (Exception e) {
            log.error("Error while consuming encode finished event: ", e);
        }
    }


}
