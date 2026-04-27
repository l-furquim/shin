package com.shin.comment.domain.usecase;

import com.shin.comment.domain.gateway.CommentGateway;
import com.shin.comment.domain.gateway.CommentMessageGateway;
import com.shin.comment.domain.gateway.ThreadGateway;
import com.shin.comment.infrastructure.dto.CommentDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteCommentUseCase {

    private final CommentGateway commentGateway;
    private final ThreadGateway threadGateway;
    private final CommentMessageGateway commentMessageGateway;


    public void execute(String userId, UUID commentId) {
        commentGateway.findById(commentId.toString()).ifPresent(comment -> {
            if (!userId.equals(comment.getAuthorId()) || comment.isDeleted()) {
                return;
            }
            comment.setDeleted(true);
            commentGateway.save(comment);
            commentMessageGateway.sendCommentDeletedEvent(
                    new CommentDeletedEvent(commentId.toString(), comment.getVideoId(), userId)
            );
        });
    }
}
