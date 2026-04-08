package com.shin.comment.producers;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ThreadCreatedProducer {

    private final SqsTemplate sqsTemplate;

    @Value("${spring.cloud.aws.queues.thread-created-queue}")
    private String queue;

    public void sendEvent(String threadId, String videoId, String channelId, String authorId) {
        try {
            sqsTemplate.send(queue, new ThreadCreatedEvent(threadId, videoId, channelId, authorId));
        } catch (Exception e) {
            log.error("Failed to publish ThreadCreated event for threadId={}: {}", threadId, e.getMessage());
            throw new InternalException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to publish event");
        }
    }
}
