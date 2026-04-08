package com.shin.comment.dto;

public record UpdateCommentRequest(
        String content,
        Long likeDelta
) {
}
