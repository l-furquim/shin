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
public class VideoLikedProducer {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.queues.like-events-queue}")
    private String queue;

    public void sendEvent(UUID videoId, UUID userId) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new VideoReactionEvent(videoId.toString(), userId.toString(), "like", "ADD"));
            sqsTemplate.send(queue, payload);
        } catch (Exception e) {
            log.error("Failed to publish VideoLiked event for videoId={}: {}", videoId, e.getMessage());
            throw new ReactionTransactionException();
        }
    }

    public void sendRemoveEvent(UUID videoId, UUID userId) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new VideoReactionEvent(videoId.toString(), userId.toString(), "like", "REMOVE"));
            sqsTemplate.send(queue, payload);
        } catch (Exception e) {
            log.error("Failed to publish VideoUnliked event for videoId={}: {}", videoId, e.getMessage());
        }
    }
}
