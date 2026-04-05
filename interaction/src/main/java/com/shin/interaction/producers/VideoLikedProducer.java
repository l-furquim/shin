package com.shin.interaction.producers;

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

    @Value("${spring.cloud.aws.queues.like-events-queue}")
    private String queue;

    public void sendEvent(UUID videoId, UUID userId) {
        try {
            sqsTemplate.send(queue, new VideoReactionEvent(videoId.toString(), userId.toString(), "like"));
        } catch (Exception e) {
            log.error("Failed to publish VideoLiked event for videoId={}: {}", videoId, e.getMessage());
            throw new ReactionTransactionException();
        }
    }
}
