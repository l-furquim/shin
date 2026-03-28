package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.ThumbnailGeneratedEvent;
import com.shin.metadata.service.VideoService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailFinishedConsumer {

    private final ObjectMapper objectMapper;
    private final VideoService videoService;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.thumbnail-finished-queue}")
    public void consume(String message) {
        try {
            ThumbnailGeneratedEvent event = objectMapper.readValue(message, ThumbnailGeneratedEvent.class);
            log.info("Received thumbnail finished message={}", event);

            videoService.updateVideoThumbnail(event.videoId(), event.s3Key());
        } catch (Exception e) {
            log.error("Error while consuming thumbnail finished event", e);
        }
    }
}
