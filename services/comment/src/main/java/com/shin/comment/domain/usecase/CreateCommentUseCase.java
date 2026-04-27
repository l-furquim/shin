package com.shin.comment.domain.usecase;

import com.shin.comment.application.dto.CreateCommentRequest;
import com.shin.comment.application.dto.CreateCommentResponse;
import com.shin.comment.domain.exceptions.ChannelConsultingException;
import com.shin.comment.domain.exceptions.InvalidCommentException;
import com.shin.comment.domain.gateway.CommentGateway;
import com.shin.comment.domain.gateway.CommentMessageGateway;
import com.shin.comment.domain.gateway.CreatorGateway;
import com.shin.comment.domain.gateway.ThreadGateway;
import com.shin.comment.domain.gateway.ThreadMessageGateway;
import com.shin.comment.domain.model.Comment;
import com.shin.comment.domain.model.Thread;
import com.shin.comment.domain.service.ContentSanitizerService;
import com.shin.comment.infrastructure.dto.CommentReplyCreatedEvent;
import com.shin.comment.infrastructure.dto.ThreadCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateCommentUseCase {

    private final CommentGateway commentGateway;
    private final ThreadGateway threadGateway;
    private final CreatorGateway creatorGateway;
    private final ContentSanitizerService contentSanitizerService;
    private final ThreadMessageGateway threadMessageGateway;
    private final CommentMessageGateway commentMessageGateway;

    public CreateCommentResponse execute(String userId, CreateCommentRequest request) {
        String sanitized = contentSanitizerService.sanitize(request.content());
        String formatted = contentSanitizerService.format(request.content());

        if (request.parentId().isPresent()) {
            boolean parentExists = commentGateway.findById(request.parentId().get()).isPresent();
            if (!parentExists) {
                throw new InvalidCommentException();
            }
        }

        CreatorGateway.CreatorInfo creatorInfo = creatorGateway.findById(UUID.fromString(userId))
                .orElseThrow(ChannelConsultingException::new);

        String now = LocalDateTime.now().toString();
        Comment comment = Comment.builder()
                .id(UUID.randomUUID().toString())
                .textOriginal(sanitized)
                .textDisplay(formatted)
                .videoId(request.videoId().toString())
                .likeCount(0L)
                .deleted(false)
                .parentId(request.parentId().orElse(null))
                .authorId(userId)
                .authorDisplayName(creatorInfo.displayName())
                .authorAvatarUrl(creatorInfo.avatarUrl())
                .authorLink(creatorInfo.link())
                .createdAt(now)
                .updatedAt(now)
                .build();

        commentGateway.save(comment);

        if (request.parentId().isEmpty()) {
            Thread thread = Thread.builder()
                    .topLevelCommentId(comment.getId())
                    .videoId(request.videoId().toString())
                    .channelId(request.channelId().toString())
                    .authorId(userId)
                    .authorDisplayName(creatorInfo.displayName())
                    .authorAvatarUrl(creatorInfo.avatarUrl())
                    .authorLink(creatorInfo.link())
                    .totalReplyCount(0L)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            threadGateway.save(thread);
            threadMessageGateway.sendThreadCreatedEvent(new ThreadCreatedEvent(
                    comment.getId(), request.videoId().toString(), request.channelId().toString(), userId
            ));
        } else {
            commentMessageGateway.sendCommentReplyCreatedEvent(new CommentReplyCreatedEvent(
                    comment.getId(), request.parentId().get(), request.videoId().toString(), userId
            ));
        }

        return new CreateCommentResponse(
                comment.getId(),
                request.parentId().isPresent() ? request.parentId().get() : comment.getId(),
                request.parentId().orElse(null),
                request.videoId().toString(),
                userId,
                comment.getAuthorDisplayName(),
                comment.getAuthorAvatarUrl(),
                comment.getAuthorLink(),
                comment.getTextOriginal(),
                comment.getTextDisplay(),
                comment.getLikeCount()
        );
    }
}
