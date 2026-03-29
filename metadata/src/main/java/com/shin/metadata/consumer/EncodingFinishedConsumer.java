package com.shin.metadata.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.metadata.dto.EncodeFinishedEvent;
import com.shin.metadata.service.VideoService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EncodingFinishedConsumer {

    private final ObjectMapper objectMapper;
    private final VideoService videoService;

    @SqsListener(queueNames = "${spring.cloud.aws.queues.encode-finished-queue}")
    public void consume(String message) {
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
            log.error("Error while consuming encoding finished event", e);
        }
    }
}
