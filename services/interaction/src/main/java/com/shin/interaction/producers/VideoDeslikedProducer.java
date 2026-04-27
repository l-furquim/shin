package com.shin.interaction.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.interaction.exceptions.ReactionTransactionException;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class VideoDeslikedProducer {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.queues.dislike-events-queue}")
    private String queue;

    public void sendEvent(UUID videoId, UUID userId) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new VideoReactionEvent(videoId.toString(), userId.toString(), "dislike", "ADD"));
            sqsTemplate.send(queue, payload);
        } catch (Exception e) {
            log.error("Failed to publish VideoDesliked event for videoId={}: {}", videoId, e.getMessage());
            throw new ReactionTransactionException();
        }
    }

    public void sendRemoveEvent(UUID videoId, UUID userId) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new VideoReactionEvent(videoId.toString(), userId.toString(), "dislike", "REMOVE"));
            sqsTemplate.send(queue, payload);
        } catch (Exception e) {
            log.error("Failed to publish VideoUndisliked event for videoId={}: {}", videoId, e.getMessage());
        }
    }
}
