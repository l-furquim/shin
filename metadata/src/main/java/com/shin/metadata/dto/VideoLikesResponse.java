package com.shin.metadata.dto;

public record VideoLikesResponse(
        Long likeCount,
        Boolean likedByMe
) {
}
