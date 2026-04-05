package com.shin.interaction.dto;

import java.time.LocalDateTime;

public record CreateReactionResponse(
        Long likesCount,
        Long deslikesCount,
        LocalDateTime interactedAt
) {
}
