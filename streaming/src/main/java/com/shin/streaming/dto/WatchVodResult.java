package com.shin.streaming.dto;

import java.util.List;

public record WatchVodResult(WatchVodResponse response, List<String> cookies) {
}
