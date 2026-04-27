package com.shin.comment.infrastructure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.comment.domain.gateway.CommentMessageGateway;
import com.shin.comment.infrastructure.dto.CommentDeletedEvent;
import com.shin.comment.infrastructure.dto.CommentReplyCreatedEvent;
import com.shin.comment.infrastructure.dto.CommentUpdatedEvent;
import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommentMessageGatewayImpl implements CommentMessageGateway {

    private final SqsTemplate sqsTemplate;
    private final SnsTemplate snsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.queues.comment-reply-created-topic}")
    private String commentReplyTopic;

    @Value("${spring.cloud.aws.queues.comment-deleted-queue}")
    private String commentDeletedQueue;

    @Value("${spring.cloud.aws.queues.comment-updated-queue}")
    private String commentUpdatedQueue;

    @Override
    public void sendCommentReplyCreatedEvent(CommentReplyCreatedEvent event) {
        try {

            SnsNotification<CommentReplyCreatedEvent> notification = SnsNotification.builder(event).build();

            snsTemplate.sendNotification(this.commentReplyTopic, notification);
        } catch (Exception e) {
            log.error("Failed to publish CommentReplyCreated event for commentId={}: {}", event.commentId(), e.getMessage());
            throw new InternalException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to publish event");
        }
    }

    @Override
    public void sendCommentDeletedEvent(CommentDeletedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            sqsTemplate.send(commentDeletedQueue, payload);
        } catch (Exception e) {
            log.error("Failed to publish CommentDeleted event for commentId={}: {}", event.commentId(), e.getMessage());
            throw new InternalException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to publish event");
        }
    }

    @Override
    public void sendCommentUpdatedEvent(CommentUpdatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            sqsTemplate.send(commentUpdatedQueue, payload);
        } catch (Exception e) {
            log.error("Failed to publish CommentUpdated event for commentId={}: {}", event.commentId(), e.getMessage());
            throw new InternalException(ErrorCodes.INTERNAL_SERVER_ERROR, "Failed to publish event");
        }
    }
}
