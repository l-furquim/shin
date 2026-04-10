package com.shin.streaming.dto;

import java.util.List;
import java.util.Map;

public record WatchVodResponse(
        VideoDetails videoDetails,
        List<Map<String, String>> manifests,
        String playbackToken
) {
}
