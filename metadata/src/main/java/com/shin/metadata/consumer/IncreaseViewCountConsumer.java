package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.IncreaseViewCountEvent;
import com.shin.metadata.service.VideoService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class IncreaseViewCountConsumer {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    private final ObjectMapper objectMapper;
    private final VideoService videoService;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.view-count-queue}")
    public void consume(String message) {
        try {
            IncreaseViewCountEvent event = resolveEvent(message);
            if (event == null) {
                return;
            }

            String viewerKey = resolveViewerKey(event);

            videoService.increaseVideoView(event.videoId(), viewerKey);

        } catch (Exception e) {
            log.error("Error while consuming view event", e);
        }
    }

    private IncreaseViewCountEvent resolveEvent(String message) throws Exception {
        JsonNode node = objectMapper.readTree(message);

        if (node.has("Message") && node.get("Message").isTextual()) {
            return objectMapper.readValue(node.get("Message").asText(), IncreaseViewCountEvent.class);
        }

        if (node.has("detail")) {
            JsonNode detail = node.get("detail");
            String objectKey = detail.path("requestParameters").path("key").asText("");
            if (!isQualifyingChunkObject(objectKey)) {
                return null;
            }
            UUID videoId = readVideoIdFromDetail(detail);
            String viewerKey = readViewerKeyFromDetail(detail, videoId);
            return new IncreaseViewCountEvent(videoId, null, viewerKey);
        }

        return objectMapper.readValue(message, IncreaseViewCountEvent.class);
    }

    private UUID readVideoIdFromDetail(JsonNode detail) {
        JsonNode videoIdNode = detail.get("videoId");
        if (videoIdNode == null || videoIdNode.asText().isBlank()) {
            JsonNode objectKey = detail.path("requestParameters").path("key");
            if (!objectKey.isMissingNode()) {
                return extractVideoIdFromObjectKey(objectKey.asText());
            }
            throw new IllegalArgumentException("videoId not found in view event");
        }
        return UUID.fromString(videoIdNode.asText());
    }

    private String readViewerKeyFromDetail(JsonNode detail, UUID videoId) {
        JsonNode viewerKeyNode = detail.get("viewerKey");
        if (viewerKeyNode != null && !viewerKeyNode.asText().isBlank()) {
            return viewerKeyNode.asText();
        }

        JsonNode userIdNode = detail.get("userId");
        if (userIdNode != null && !userIdNode.asText().isBlank()) {
            return userIdNode.asText();
        }

        String sourceIp = detail.path("sourceIPAddress").asText("");
        String userAgent = detail.path("userAgent").asText("");
        String principalId = detail.path("userIdentity").path("principalId").asText("");
        return sourceIp + "|" + userAgent + "|" + principalId + "|" + videoId;
    }

    private String resolveViewerKey(IncreaseViewCountEvent event) {
        if (event.viewerKey() != null && !event.viewerKey().isBlank()) {
            return event.viewerKey();
        }
        if (event.userId() != null) {
            return event.userId().toString();
        }
        throw new IllegalArgumentException("viewerKey/userId not found in view event");
    }

    private UUID extractVideoIdFromObjectKey(String objectKey) {
        String decodedKey = URLDecoder.decode(objectKey, StandardCharsets.UTF_8);
        String[] parts = decodedKey.split("/");
        for (String part : parts) {
            if (UUID_PATTERN.matcher(part).matches()) {
                return UUID.fromString(part);
            }
        }
        throw new IllegalArgumentException("Could not extract videoId from object key: " + decodedKey);
    }

    private boolean isQualifyingChunkObject(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }

        String normalized = objectKey.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".m4s") && normalized.contains("chunk-");
    }


}
