package com.shin.comment.infrastructure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shin.comment.domain.gateway.CommentMessageGateway;
import com.shin.comment.infrastructure.dto.CommentDeletedEvent;
import com.shin.comment.infrastructure.dto.CommentReplyCreatedEvent;
import com.shin.comment.infrastructure.dto.CommentUpdatedEvent;
import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommentMessageGatewayImpl implements CommentMessageGateway {

    private static final Logger log = LoggerFactory.getLogger(CommentMessageGatewayImpl.class);

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.queues.comment-reply-created-queue}")
    private String commentReplyCreatedQueue;

    @Value("${spring.cloud.aws.queues.comment-deleted-queue}")
    private String commentDeletedQueue;

    @Value("${spring.cloud.aws.queues.comment-updated-queue}")
    private String commentUpdatedQueue;

    public CommentMessageGatewayImpl(
            SqsTemplate sqsTemplate,
            ObjectMapper objectMapper
    ) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendCommentReplyCreatedEvent(CommentReplyCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            sqsTemplate.send(commentReplyCreatedQueue, payload);
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
