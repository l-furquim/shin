package com.shin.streaming.dto;

import java.time.LocalDateTime;

public record PlaybackProgressEvent(
    String sessionId,
    String videoId,
    String userId,
    Long watchTimeSeconds,
    Long currentPositionSeconds,
    LocalDateTime eventTimestamp
) {

}
