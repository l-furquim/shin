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
public class CommentUpdatedProducer {

    private final SqsTemplate sqsTemplate;

    @Value("${spring.cloud.aws.queues.comment-updated-queue}")
    private String queue;

    public void sendEvent(String commentId, String videoId, String authorId) {
        try {
            sqsTemplate.send(queue, new CommentUpdatedEvent(commentId, videoId, authorId));
        } catch (Exception e) {
            log.error("Failed to publish CommentUpdated event for commentId={}: {}", commentId, e.getMessage());
            throw new InternalException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to publish event");
        }
    }
}
