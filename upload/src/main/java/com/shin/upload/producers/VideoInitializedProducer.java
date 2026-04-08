package com.shin.upload.producers;

import com.shin.upload.dto.VideoInitializedEvent;
import com.shin.upload.exceptions.InvalidVideoUploadException;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoInitializedProducer {

    private final SqsTemplate template;

    @Value("${spring.cloud.aws.queues.video-initialized-queue}")
    private String queue;

    public void send(VideoInitializedEvent event) {
        try {
            template.send(queue, event);
            log.info("Published VideoInitializedEvent videoId={}", event.videoId());
        } catch (Exception e) {
            log.error("Failed to publish VideoInitializedEvent videoId={}: {}", event.videoId(), e.getMessage());
            throw new InvalidVideoUploadException("Failed to initialize video: " + e.getMessage());
        }
    }
}
