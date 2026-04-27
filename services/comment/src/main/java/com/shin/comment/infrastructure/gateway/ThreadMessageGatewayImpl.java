package com.shin.comment.infrastructure.gateway;

import com.shin.comment.domain.gateway.ThreadMessageGateway;
import com.shin.comment.infrastructure.dto.ThreadCreatedEvent;
import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ThreadMessageGatewayImpl implements ThreadMessageGateway {

    private final SnsTemplate snsTemplate;

    @Value("${spring.cloud.aws.queues.thread-created-topic}")
    private String threadCreatedTopic;

    @Override
    public void sendThreadCreatedEvent(ThreadCreatedEvent event) {
        try {
            SnsNotification<ThreadCreatedEvent> notification = SnsNotification.builder(event).build();

            snsTemplate.sendNotification(this.threadCreatedTopic, notification);
        } catch (Exception e) {
            log.error("Failed to publish ThreadCreated event for threadId={}: {}", event.threadId(), e.getMessage());
            throw new InternalException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to publish event");
        }
    }
}
