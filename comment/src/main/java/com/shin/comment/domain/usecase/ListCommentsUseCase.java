package com.shin.comment.domain.usecase;

import com.shin.comment.application.dto.CommentDto;
import com.shin.comment.application.dto.CommentListResponse;
import com.shin.comment.domain.exceptions.InvalidCommentException;
import com.shin.comment.domain.gateway.CommentGateway;
import com.shin.comment.domain.model.Comment;
import com.shin.comment.infrastructure.dto.CommentScanPage;
import com.shin.commons.models.PageInfo;
import com.shin.commons.util.PageTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListCommentsUseCase {

    private final CommentGateway commentGateway;

    public CommentListResponse execute(
            List<String> ids, String parentId, int maxResults, String pageToken, String textFormat) {

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
            comments = commentGateway.findByIds(ids);
        } else {
            Map<String, AttributeValue> startKey = null;
            if (pageToken != null && !pageToken.isBlank()) {
                startKey = PageTokenUtil.decode(pageToken).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> AttributeValue.fromS(e.getValue())));
            }

            CommentScanPage page = commentGateway.findByParentId(parentId, maxResults + 1, startKey);
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
                .map(c -> toDto(c, plainText))
                .toList();

        return new CommentListResponse(nextPageToken, new PageInfo((long) items.size(), (long) maxResults), items);
    }

    private CommentDto toDto(Comment c, boolean plainText) {
        String text = plainText
                ? c.getTextDisplay().replaceAll("<[^>]*>", "")
                : c.getTextDisplay();
        return new CommentDto(
                c.getId(), c.getParentId(), c.getVideoId(),
                c.getAuthorId(), c.getAuthorDisplayName(), c.getAuthorAvatarUrl(), c.getAuthorLink(),
                text, c.getTextOriginal(), c.getLikeCount(), c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
