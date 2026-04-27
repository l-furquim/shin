package com.shin.streaming.dto;

public record LastWatchVod(
        long accumulatedWatchTime,
        boolean viewCounted
) {
}
