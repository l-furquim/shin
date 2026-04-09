package com.shin.streaming.dto;

import jakarta.validation.constraints.Positive;

public record ViewEventRequest(
        String playbackSessionToken,
        @Positive Long watchTimeSeconds,
        @Positive Long currentPositionSeconds,
        @Positive Long totalDurationSeconds
) {
}
