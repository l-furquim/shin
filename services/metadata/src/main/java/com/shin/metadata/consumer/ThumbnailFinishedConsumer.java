package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.ThumbnailGeneratedEvent;
import com.shin.metadata.service.VideoService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailFinishedConsumer {

    private static final String PROCESSED_KEY_PREFIX = "processed:";
    private static final Duration DEDUP_TTL = Duration.ofDays(4);

    private final ObjectMapper objectMapper;
    private final VideoService videoService;
    private final StringRedisTemplate stringRedisTemplate;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.thumbnail-finished-queue}")
    public void consume(String message, @Header("id") String messageId) {
        String dedupKey = PROCESSED_KEY_PREFIX + messageId;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Skipping duplicate thumbnail finished event messageId={}", messageId);
            return;
        }

        try {
            ThumbnailGeneratedEvent event = objectMapper.readValue(message, ThumbnailGeneratedEvent.class);
            log.info("Received thumbnail finished message={}", event);
            videoService.updateVideoThumbnail(event.videoId(), event.s3Key());
        } catch (Exception e) {
            stringRedisTemplate.delete(dedupKey);
            log.error("Error while consuming thumbnail finished event", e);
        }
    }
}
