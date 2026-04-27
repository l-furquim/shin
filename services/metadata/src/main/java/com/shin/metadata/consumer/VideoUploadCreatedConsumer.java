package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.client.UserServiceClient;
import com.shin.metadata.dto.CreateVideoRequest;
import com.shin.metadata.dto.CreatorResponse;
import com.shin.metadata.dto.RawUploadCreatedEvent;
import com.shin.metadata.model.enums.TranscodingStatus;
import com.shin.metadata.model.enums.VideoVisibility;
import com.shin.metadata.repository.VideoRepository;
import com.shin.metadata.service.VideoService;
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
public class VideoUploadCreatedConsumer {

    private static final String PROCESSED_KEY_PREFIX = "processed:";
    private static final Duration DEDUP_TTL = Duration.ofDays(4);

    private final ObjectMapper objectMapper;
    private final VideoService videoService;
    private final VideoRepository videoRepository;
    private final UserServiceClient userServiceClient;
    private final StringRedisTemplate stringRedisTemplate;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.video-upload-created-queue}")
    public void consume(String message, @Header("id") String messageId) {
        String dedupKey = PROCESSED_KEY_PREFIX + messageId;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Skipping duplicate video upload created event messageId={}", messageId);
            return;
        }

        try {
            RawUploadCreatedEvent event = objectMapper.readValue(message, RawUploadCreatedEvent.class);
            log.info("Received VideoUploadCreatedEvent videoId={}", event.videoId());

            VideoVisibility visibility = parseVisibility(event.visibility());
            videoService.createVideo(new CreateVideoRequest(
                    UUID.fromString(event.videoId()),
                    event.title() != null ? event.title() : "Untitled",
                    "",
                    visibility,
                    TranscodingStatus.PROCESSING,
                    event.userId(),
                    event.resolutions(),
                    event.s3Key(),
                    event.fileName(),
                    event.fileSize(),
                    event.contentType()
            ));

            enrichWithCreatorInfo(UUID.fromString(event.videoId()), UUID.fromString(event.userId()));
        } catch (Exception e) {
            stringRedisTemplate.delete(dedupKey);
            log.error("Error consuming VideoUploadCreatedEvent messageId={}", messageId, e);
        }
    }

    private void enrichWithCreatorInfo(UUID videoId, UUID creatorId) {
        try {
            CreatorResponse creator = userServiceClient.getCreatorById(creatorId);
            videoRepository.findById(videoId).ifPresent(video -> {
                video.setCreatorDisplayName(creator.displayName());
                video.setCreatorAvatarUrl(creator.avatar());
                videoRepository.save(video);
            });
        } catch (Exception e) {
            log.debug("Could not fetch creator info for creatorId={} on video upload created", creatorId);
        }
    }

    private VideoVisibility parseVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return VideoVisibility.PRIVATE;
        }
        try {
            return VideoVisibility.valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            return VideoVisibility.PRIVATE;
        }
    }
}
