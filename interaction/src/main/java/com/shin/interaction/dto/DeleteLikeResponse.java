package com.shin.interaction.dto;

public record DeleteLikeResponse(
        Long likesCount,
        boolean likedByMe
) {
}
