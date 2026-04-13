package com.shin.comment.infrastructure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.comment.domain.gateway.ThreadMessageGateway;
import com.shin.comment.infrastructure.dto.ThreadCreatedEvent;
import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ThreadMessageGatewayImpl implements ThreadMessageGateway {

    private static final Logger log = LoggerFactory.getLogger(ThreadMessageGatewayImpl.class);

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.queues.thread-created-queue}")
    private String threadCreatedQueue;

    public ThreadMessageGatewayImpl(SqsTemplate sqsTemplate, ObjectMapper objectMapper) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendThreadCreatedEvent(ThreadCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            sqsTemplate.send(threadCreatedQueue, payload);
        } catch (Exception e) {
            log.error("Failed to publish ThreadCreated event for threadId={}: {}", event.threadId(), e.getMessage());
            throw new InternalException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to publish event");
        }
    }
}
