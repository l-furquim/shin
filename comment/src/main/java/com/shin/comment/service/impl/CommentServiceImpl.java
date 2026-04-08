package com.shin.comment.service.impl;

import com.shin.comment.dto.CommentDto;
import com.shin.comment.dto.CommentListResponse;
import com.shin.comment.dto.CreateCommentRequest;
import com.shin.comment.dto.CreateCommentResponse;
import com.shin.comment.dto.UpdateCommentRequest;
import com.shin.comment.dto.UpdateCommentResponse;
import com.shin.comment.exception.CommentNotFoundException;
import com.shin.comment.exception.ForbiddenCommentOperationException;
import com.shin.comment.exception.InvalidCommentException;
import com.shin.comment.model.Comment;
import com.shin.comment.producers.CommentDeletedProducer;
import com.shin.comment.producers.CommentReplyCreatedProducer;
import com.shin.comment.producers.CommentUpdatedProducer;
import com.shin.comment.producers.ThreadCreatedProducer;
import com.shin.comment.repository.CommentRepository;
import com.shin.comment.repository.CommentScanPage;
import com.shin.comment.service.CommentService;
import com.shin.comment.service.ContentSanitizerService;
import com.shin.comment.service.ThreadService;
import com.shin.commons.util.PageTokenUtil;
import com.shin.commons.models.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {

    private final ContentSanitizerService contentSanitizerService;
    private final ThreadService threadService;
    private final CommentRepository commentRepository;
    private final ThreadCreatedProducer threadCreatedProducer;
    private final CommentReplyCreatedProducer commentReplyCreatedProducer;
    private final CommentUpdatedProducer commentUpdatedProducer;
    private final CommentDeletedProducer commentDeletedProducer;

    @Override
    public CreateCommentResponse createComment(String userId, CreateCommentRequest request) {

        final var contentSanitized = this.contentSanitizerService.sanitize(request.content());
        final var contentFormatted = this.contentSanitizerService.format(request.content());

        if (request.parentId().isPresent()) {
            final var parentExists = this.commentRepository.findById(request.parentId().get()).isPresent();

            if (!parentExists) {
                throw new InvalidCommentException();
            }
        }

        final var comment = Comment.builder()
                .id(UUID.randomUUID().toString())
                .textOriginal(contentSanitized)
                .textDisplay(contentFormatted)
                .videoId(request.videoId().toString())
                .likeCount(0L)
                .deleted(false)
                .parentId(request.parentId().orElse(null))
                .updatedAt(LocalDateTime.now().toString())
                .authorId(userId)
                .createdAt(LocalDateTime.now().toString())
                .build();

        commentRepository.save(comment);

        if (request.parentId().isEmpty()) {
            this.threadService.create(
                    comment.getId(), userId,
                    request.channelId().toString(),
                    request.videoId().toString());
            threadCreatedProducer.sendEvent(
                    comment.getId(), request.videoId().toString(),
                    request.channelId().toString(), userId);
        } else {
            commentReplyCreatedProducer.sendEvent(
                    comment.getId(), request.parentId().get(),
                    request.videoId().toString(), userId);
        }

        return new CreateCommentResponse(
                comment.getId(),
                request.parentId().isPresent() ? request.parentId().toString() : comment.getId(),
                request.parentId().orElse(null),
                request.videoId().toString(),
                userId,
                comment.getTextOriginal(),
                comment.getTextDisplay(),
                comment.getLikeCount()
        );
    }

    @Override
    public UpdateCommentResponse updateComment(String userId, UUID commentId, UpdateCommentRequest request) {
        if (request.likeDelta() != null && (!request.likeDelta().equals(1L) || !request.likeDelta().equals(-1L))) {
            throw new InvalidCommentException();
        }

        final var comment = this.commentRepository.findById(commentId.toString());

        if (comment.isEmpty()) {
            throw new CommentNotFoundException();
        }

        if (!userId.equals(comment.get().getAuthorId())) {
            throw new ForbiddenCommentOperationException();
        }

        if (comment.get().isDeleted()) {
            throw new ForbiddenCommentOperationException();
        }

        var changed = false;

        if (!request.content().isBlank()) {
            final var contentSanitized = this.contentSanitizerService.sanitize(request.content());
            final var contentFormatted = this.contentSanitizerService.format(request.content());

            comment.get().setTextOriginal(contentSanitized);
            comment.get().setTextDisplay(contentFormatted);
            changed = true;
        }

        if (request.likeDelta() != null) {
            comment.get().applyLikeDelta(request.likeDelta());
            changed = true;
        }

        if (changed) {
            comment.get().setUpdatedAt(LocalDateTime.now().toString());
            commentRepository.save(comment.get());
            commentUpdatedProducer.sendEvent(commentId.toString(), comment.get().getVideoId(), userId);
        }

        return new UpdateCommentResponse(
                comment.get().getId(),
                comment.get().getParentId() != null ? comment.get().getParentId() : comment.get().getId(),
                comment.get().getParentId(),
                comment.get().getVideoId(),
                userId,
                comment.get().getTextOriginal(),
                comment.get().getTextDisplay(),
                comment.get().getLikeCount()
        );
    }

    @Override
    public void deleteComment(String userId, UUID commentId) {
        final var comment = this.commentRepository.findById(commentId.toString());

        if (comment.isEmpty() || !userId.equals(comment.get().getAuthorId()) || comment.get().isDeleted()) {
            return;
        }

        comment.get().setDeleted(true);
        commentRepository.save(comment.get());
        commentDeletedProducer.sendEvent(commentId.toString(), comment.get().getVideoId(), userId);
    }

    @Override
    public CommentListResponse listComments(
            List<String> ids,
            String parentId,
            int maxResults,
            String pageToken,
            String textFormat) {

        boolean hasIds = ids != null && !ids.isEmpty();
        boolean hasParentId = parentId != null && !parentId.isBlank();

        if (hasIds == hasParentId) {
            throw new InvalidCommentException();
        }

        if (maxResults < 1 || maxResults > 100) {
            throw new InvalidCommentException();
        }

        List<Comment> comments;
        String nextPageToken = null;

        if (hasIds) {
            if (ids.size() > 100) {
                throw new InvalidCommentException();
            }
            comments = commentRepository.findByIds(ids);
        } else {
            Map<String, AttributeValue> startKey = null;
            if (pageToken != null && !pageToken.isBlank()) {
                startKey = PageTokenUtil.decode(pageToken).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> AttributeValue.fromS(e.getValue())));
            }

            CommentScanPage page = commentRepository.findByParentId(parentId, maxResults + 1, startKey);
            List<Comment> fetched = new ArrayList<>(page.items());

            boolean hasMore = fetched.size() > maxResults
                    || (page.lastEvaluatedKey() != null && !page.lastEvaluatedKey().isEmpty());

            if (fetched.size() > maxResults) {
                fetched = fetched.subList(0, maxResults);
            }
            comments = fetched;

            if (hasMore && !comments.isEmpty()) {
                Comment last = comments.getLast();
                nextPageToken = PageTokenUtil.encode(
                        "id", last.getId(),
                        "createdAt", last.getCreatedAt(),
                        "parentId", parentId
                );
            }
        }

        boolean plainText = "plainText".equalsIgnoreCase(textFormat);
        List<CommentDto> items = comments.stream()
                .map(c -> toCommentDto(c, plainText))
                .toList();

        return new CommentListResponse(
                nextPageToken,
                new PageInfo((long) items.size(), (long) maxResults),
                items
        );
    }

    private CommentDto toCommentDto(Comment c, boolean plainText) {
        String text = plainText
                ? c.getTextDisplay().replaceAll("<[^>]*>", "")
                : c.getTextDisplay();
        return new CommentDto(
                c.getId(),
                c.getParentId(),
                c.getVideoId(),
                c.getAuthorId(),
                text,
                c.getTextOriginal(),
                c.getLikeCount(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
