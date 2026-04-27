package com.shin.comment.domain.usecase;

import com.shin.comment.application.dto.UpdateCommentRequest;
import com.shin.comment.application.dto.UpdateCommentResponse;
import com.shin.comment.domain.exceptions.CommentNotFoundException;
import com.shin.comment.domain.exceptions.ForbiddenCommentOperationException;
import com.shin.comment.domain.gateway.CommentGateway;
import com.shin.comment.domain.gateway.CommentMessageGateway;
import com.shin.comment.domain.service.ContentSanitizerService;
import com.shin.comment.infrastructure.dto.CommentUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateCommentUseCase {

    private final CommentGateway commentGateway;
    private final ContentSanitizerService contentSanitizerService;
    private final CommentMessageGateway commentMessageGateway;

    public UpdateCommentResponse execute(String userId, UUID commentId, UpdateCommentRequest request) {
        var comment = commentGateway.findById(commentId.toString())
                .orElseThrow(CommentNotFoundException::new);

        if (!userId.equals(comment.getAuthorId()) || comment.isDeleted()) {
            throw new ForbiddenCommentOperationException();
        }

        boolean changed = false;

        if (request.content() != null && !request.content().isBlank()) {
            comment.setTextOriginal(contentSanitizerService.sanitize(request.content()));
            comment.setTextDisplay(contentSanitizerService.format(request.content()));
            changed = true;
        }

        if (request.likeDelta() != null) {
            comment.applyLikeDelta(request.likeDelta());
            changed = true;
        }

        if (changed) {
            comment.setUpdatedAt(LocalDateTime.now().toString());
            commentGateway.save(comment);
            commentMessageGateway.sendCommentUpdatedEvent(
                    new CommentUpdatedEvent(commentId.toString(), comment.getVideoId(), userId)
            );
        }

        return new UpdateCommentResponse(
                comment.getId(),
                comment.getParentId() != null ? comment.getParentId() : comment.getId(),
                comment.getParentId(),
                comment.getVideoId(),
                userId,
                comment.getTextOriginal(),
                comment.getTextDisplay(),
                comment.getLikeCount()
        );
    }
}
