package com.shin.streaming.producer;

import com.shin.streaming.dto.PlaybackProgressEvent;
import com.shin.streaming.exception.PlaybackEventProducerException;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class PlaybackProgressProducer {

    private final SqsTemplate sqsTemplate;

    @Value("${spring.cloud.aws.queues.playback-progress-queue}")
    private String queue;

    public void sendEvent(PlaybackProgressEvent playbackProgressEvent) {
        try {
            sqsTemplate.send(queue, playbackProgressEvent);
        } catch (Exception e) {
            log.error("Failed to publish playback event for videoId={}: {}", playbackProgressEvent.videoId(), e.getMessage());
            throw new PlaybackEventProducerException();
        }
    }
}
