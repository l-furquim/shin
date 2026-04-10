package com.shin.search.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.search.dto.VideoPublishedEvent;
import com.shin.search.service.SearchService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class VideoPublishedConsumer {

    private final ObjectMapper objectMapper;
    private final SearchService searchService;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.video-published-queue}")
    public void consume(String message) {

        try {
            VideoPublishedEvent event = unwrapAndDeserialize(message);
            if (event == null) {
                log.warn("Could not deserialize VideoPublishedEvent");
                return;
            }

            searchService.indexVideo(event);
        } catch (Exception e) {
            log.error("Error indexing video published: {}", e.getMessage(), e);
        }
    }

    private VideoPublishedEvent unwrapAndDeserialize(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);

            String payload;
            if (root.hasNonNull("Message") && root.get("Message").isTextual()) {
                payload = root.get("Message").asText();
            } else {
                payload = raw;
            }

            return objectMapper.readValue(payload, VideoPublishedEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse VideoPublishedEvent: {}", e.getMessage());
            return null;
        }
    }
}
