package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.RawUploadCreatedEvent;
import com.shin.metadata.service.S3MetadataService;
import com.shin.metadata.service.VideoService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class RawUploadCreatedConsumer {

    private static final String PROCESSED_KEY_PREFIX = "processed:";
    private static final Duration DEDUP_TTL = Duration.ofDays(4);

    private final ObjectMapper objectMapper;
    private final VideoService videoService;
    private final S3MetadataService s3MetadataService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${spring.cloud.aws.s3.buckets.raw}")
    private String rawBucketName;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.raw-upload-metadata-queue}")
    public void consume(String message, @Header("id") String messageId) {
        String dedupKey = PROCESSED_KEY_PREFIX + messageId;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Skipping duplicate raw upload created event messageId={}", messageId);
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(message);
            if (root.hasNonNull("videoId") && root.hasNonNull("s3Key")) {
                RawUploadCreatedEvent event = objectMapper.treeToValue(root, RawUploadCreatedEvent.class);
                videoService.updateVideoRawUploadMetadata(event);
                return;
            }

            JsonNode payload = root;
            if (root.hasNonNull("Message") && root.get("Message").isTextual()) {
                payload = objectMapper.readTree(root.get("Message").asText());
            }

            JsonNode records = payload.get("Records");
            if (records == null || !records.isArray() || records.isEmpty()) {
                log.warn("Raw upload event without records: {}", message);
                return;
            }

            JsonNode record = records.get(0);
            String eventSource = record.path("eventSource").asText();
            String eventName = record.path("eventName").asText();
            if (!eventSource.startsWith("aws:s3") || !eventName.startsWith("ObjectCreated:")) {
                log.warn("Ignoring non ObjectCreated event: source={}, event={}", eventSource, eventName);
                return;
            }

            String objectKey = URLDecoder.decode(
                    record.path("s3").path("object").path("key").asText(),
                    StandardCharsets.UTF_8
            );
            String bucketName = record.path("s3").path("bucket").path("name").asText();
            if (bucketName == null || bucketName.isBlank()) {
                bucketName = rawBucketName;
            }

            Map<String, String> metadata = s3MetadataService.getObjectMetadata(bucketName, objectKey);

            RawUploadCreatedEvent event = new RawUploadCreatedEvent(
                    metadata.get("videoid"),
                    metadata.get("userid"),
                    objectKey,
                    metadata.get("filename"),
                    metadata.get("resolutions"),
                    metadata.get("contenttype"),
                    parseLong(metadata.get("filesize"))
            );

            videoService.updateVideoRawUploadMetadata(event);
        } catch (Exception e) {
            stringRedisTemplate.delete(dedupKey);
            log.error("Error while consuming raw upload created event", e);
        }
    }

    private Long parseLong(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
