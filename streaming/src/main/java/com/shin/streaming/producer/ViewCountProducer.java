package com.shin.streaming.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.streaming.dto.ViewCountEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class ViewCountProducer {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.queues.view-events-queue}")
    private String queue;

    public void send(UUID videoId, UUID userId) {
        try {
            ViewCountEvent event = new ViewCountEvent(videoId, userId, userId.toString());
            sqsTemplate.send(queue, objectMapper.writeValueAsString(event));
            log.info("View count event sent videoId={} userId={}", videoId, userId);
        } catch (Exception e) {
            log.error("Failed to send view count event videoId={}: {}", videoId, e.getMessage());
        }
    }
}
