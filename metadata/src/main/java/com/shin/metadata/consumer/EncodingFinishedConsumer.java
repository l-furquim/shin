package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.EncodeFinishedEvent;
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
public class EncodingFinishedConsumer {

    private static final String PROCESSED_KEY_PREFIX = "processed:";
    private static final Duration DEDUP_TTL = Duration.ofDays(4);

    private final ObjectMapper objectMapper;
    private final VideoService videoService;
    private final StringRedisTemplate stringRedisTemplate;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.encoding-finished-queue}")
    public void consume(String message, @Header("id") String messageId) {
        String dedupKey = PROCESSED_KEY_PREFIX + messageId;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Skipping duplicate encoding finished event messageId={}", messageId);
            return;
        }

        try {
            EncodeFinishedEvent encodeEvent = objectMapper.readValue(message, EncodeFinishedEvent.class);
            log.info("Received encoding finished message={}", encodeEvent);

            videoService.updateVideoProcessingStatus(
                    encodeEvent.videoId(),
                    encodeEvent.status(),
                    encodeEvent.processedPath(),
                    encodeEvent.resolutions(),
                    encodeEvent.duration(),
                    encodeEvent.fileName(),
                    encodeEvent.fileSize(),
                    encodeEvent.fileType()
            );
        } catch (Exception e) {
            stringRedisTemplate.delete(dedupKey);
            log.error("Error while consuming encoding finished event", e);
        }
    }
}
