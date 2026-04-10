package com.shin.comment.dto;


public record CreateCommentResponse(
        String id,
        String threadId,
        String parentId,
        String videoId,
        String authorId,
        String authorDisplayName,
        String authorAvatarUrl,
        String authorUrl,
        String textOriginal,
        String textDisplay,
        Long likeCount
) {
}
