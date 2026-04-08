package com.shin.comment.dto;

public record CommentDto(
        String id,
        String parentId,
        String videoId,
        String authorId,
        String textDisplay,
        String textOriginal,
        Long likeCount,
        String createdAt,
        String updatedAt
) {}
