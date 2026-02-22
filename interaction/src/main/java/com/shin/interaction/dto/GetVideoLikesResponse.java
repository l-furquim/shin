package com.shin.interaction.dto;

public record GetVideoLikesResponse(
    Long likesCount,
    boolean likedByMe
) {
}
