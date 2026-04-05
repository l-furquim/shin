package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.CreateVideoRequest;
import com.shin.metadata.model.enums.ProcessingStatus;
import com.shin.metadata.model.enums.VideoVisibility;
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
 * Consumes VideoInitializedEvent from the upload service.
 * Creates the initial video record in PostgreSQL so uploads are no longer
 * blocked by a synchronous call to the metadata service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoInitializedConsumer {

    private static final String PROCESSED_KEY_PREFIX = "processed:";
    private static final Duration DEDUP_TTL = Duration.ofDays(4);

    private final ObjectMapper objectMapper;
    private final VideoService videoService;
    private final StringRedisTemplate stringRedisTemplate;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.video-initialized-queue}")
    public void consume(String message, @Header("id") String messageId) {
        String dedupKey = PROCESSED_KEY_PREFIX + messageId;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Skipping duplicate video initialized event messageId={}", messageId);
            return;
        }

        try {
            VideoInitializedEvent event = objectMapper.readValue(message, VideoInitializedEvent.class);
            log.info("Received VideoInitializedEvent videoId={}", event.videoId());

            videoService.createVideo(new CreateVideoRequest(
                    UUID.fromString(event.videoId()),
                    event.title(),
                    "",
                    VideoVisibility.valueOf(event.visibility()),
                    ProcessingStatus.valueOf(event.status()),
                    event.userId(),
                    event.resolutions()
            ));
        } catch (Exception e) {
            stringRedisTemplate.delete(dedupKey);
            log.error("Error consuming VideoInitializedEvent messageId={}", messageId, e);
        }
    }

    private record VideoInitializedEvent(
            String videoId,
            String userId,
            String title,
            String visibility,
            String status,
            String resolutions
    ) {}
}
