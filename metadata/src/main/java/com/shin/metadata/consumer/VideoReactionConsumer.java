package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.service.VideoService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Consumes VideoLiked and VideoDesliked events from the interaction service
 * to keep video.likeCount in PostgreSQL as the authoritative source of truth.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoReactionConsumer {

    private static final String PROCESSED_KEY_PREFIX = "processed:";
    private static final Duration DEDUP_TTL = Duration.ofDays(4);

    private final ObjectMapper objectMapper;
    private final VideoService videoService;
    private final StringRedisTemplate stringRedisTemplate;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.like-events-queue}")
    public void consumeLiked(String message, @Header("id") String messageId) {
        if (isDuplicate(messageId)) return;
        try {
            VideoReactionEvent event = objectMapper.readValue(message, VideoReactionEvent.class);
            log.info("Received VideoLiked event videoId={}", event.videoId());
            videoService.applyLikeDelta(UUID.fromString(event.videoId()), 1L);
        } catch (Exception e) {
            removeDedupKey(messageId);
            log.error("Error consuming VideoLiked event messageId={}", messageId, e);
        }
    }

    @SqsListener(queueNames = "${spring.cloud.aws.queues.dislike-events-queue}")
    public void consumeDesliked(String message, @Header("id") String messageId) {
        if (isDuplicate(messageId)) return;
        try {
            VideoReactionEvent event = objectMapper.readValue(message, VideoReactionEvent.class);
            log.info("Received VideoDesliked event videoId={}", event.videoId());
            videoService.applyLikeDelta(UUID.fromString(event.videoId()), -1L);
        } catch (Exception e) {
            removeDedupKey(messageId);
            log.error("Error consuming VideoDesliked event messageId={}", messageId, e);
        }
    }

    private boolean isDuplicate(String messageId) {
        String dedupKey = PROCESSED_KEY_PREFIX + messageId;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Skipping duplicate reaction event messageId={}", messageId);
            return true;
        }
        return false;
    }

    private void removeDedupKey(String messageId) {
        stringRedisTemplate.delete(PROCESSED_KEY_PREFIX + messageId);
    }

    private record VideoReactionEvent(String videoId, String userId, String reactionType) {}
}
