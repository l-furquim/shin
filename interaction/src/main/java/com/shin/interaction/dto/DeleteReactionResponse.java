package com.shin.interaction.dto;

public record DeleteReactionResponse(
        Long likesCount,
        Long deslikesCount
) {
}
