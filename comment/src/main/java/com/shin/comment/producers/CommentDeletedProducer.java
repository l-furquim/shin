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
public class CommentDeletedProducer {

    private final SqsTemplate sqsTemplate;

    @Value("${spring.cloud.aws.queues.comment-deleted-queue}")
    private String queue;

    public void sendEvent(String commentId, String videoId, String authorId) {
        try {
            sqsTemplate.send(queue, new CommentDeletedEvent(commentId, videoId, authorId));
        } catch (Exception e) {
            log.error("Failed to publish CommentDeleted event for commentId={}: {}", commentId, e.getMessage());
            throw new InternalException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to publish event");
        }
    }
}
