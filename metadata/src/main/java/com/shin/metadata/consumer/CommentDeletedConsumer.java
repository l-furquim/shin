package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.repository.VideoRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentDeletedConsumer {

    private static final String PROCESSED_KEY_PREFIX = "processed:comment-deleted:";
    private static final Duration DEDUP_TTL = Duration.ofDays(4);

    private final ObjectMapper objectMapper;
    private final VideoRepository videoRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.comment-deleted-queue}")
    @Transactional
    public void consume(String message, @Header("id") String messageId) {
        String dedupKey = PROCESSED_KEY_PREFIX + messageId;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Skipping duplicate comment-deleted event messageId={}", messageId);
            return;
        }

        try {
            CommentDeletedEvent event = objectMapper.readValue(message, CommentDeletedEvent.class);
            UUID videoId = UUID.fromString(event.videoId());

            videoRepository.findById(videoId).ifPresent(video -> {
                video.setCommentCount(Math.max(0, video.getCommentCount() - 1));
                videoRepository.save(video);
            });
        } catch (Exception e) {
            stringRedisTemplate.delete(dedupKey);
            log.error("Error consuming comment-deleted event messageId={}", messageId, e);
        }
    }

    private record CommentDeletedEvent(String commentId, String videoId, String authorId) {}
}
