package com.shin.streaming.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record ViewEventRequest(
        String playbackSessionToken,
        @Positive Long watchTimeSeconds,
        @Min(0) Long currentPositionSeconds,
        @Min(0) Long totalDurationSeconds
) {
}
