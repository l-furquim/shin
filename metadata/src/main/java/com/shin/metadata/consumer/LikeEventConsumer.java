package com.shin.metadata.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.repository.VideoRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventConsumer {

    private static final String PROCESSED_KEY_PREFIX = "processed:like:";
    private static final Duration DEDUP_TTL = Duration.ofDays(4);

    private final ObjectMapper objectMapper;
    private final VideoRepository videoRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.like-events-queue}")
    public void consume(String message, @Header("id") String messageId) {
        String dedupKey = PROCESSED_KEY_PREFIX + messageId;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Skipping duplicate like event messageId={}", messageId);
            return;
        }

        try {
            VideoReactionEvent event = objectMapper.readValue(message, VideoReactionEvent.class);
            UUID videoId = UUID.fromString(event.videoId());
            boolean isAdd = "ADD".equalsIgnoreCase(event.action());

            if (isAdd) {
                videoRepository.incrementLikeCount(videoId);
            } else {
                videoRepository.decrementLikeCount(videoId);
            }
            log.info("Like count updated videoId={} action={}", videoId, event.action());
        } catch (JsonProcessingException e) {
            stringRedisTemplate.delete(dedupKey);
            log.error("Error processing like event messageId={}", messageId, e);
        } catch (Exception e) {
            stringRedisTemplate.delete(dedupKey);
            log.error("Error consuming like event messageId={}", messageId, e);
            throw e;
        }
    }

    private record VideoReactionEvent(String videoId, String userId, String reactionType, String action) {}
}
