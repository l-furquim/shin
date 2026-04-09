package com.shin.streaming.dto;

public record WatchVodResponse(
        VideoDetails videoDetails,
        String manifestUrl,
        String playbackToken
) {
}
