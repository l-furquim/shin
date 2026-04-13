package com.shin.comment.domain.gateway;

import com.shin.comment.infrastructure.dto.CommentDeletedEvent;
import com.shin.comment.infrastructure.dto.CommentReplyCreatedEvent;
import com.shin.comment.infrastructure.dto.CommentUpdatedEvent;

public interface CommentMessageGateway {

    void sendCommentReplyCreatedEvent(CommentReplyCreatedEvent commentReplyCreatedEvent);
    void sendCommentDeletedEvent(CommentDeletedEvent event);
    void sendCommentUpdatedEvent(CommentUpdatedEvent commentUpdatedEvent);

}
