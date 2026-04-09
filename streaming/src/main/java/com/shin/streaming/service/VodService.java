package com.shin.streaming.service;

import com.shin.streaming.dto.ViewEventRequest;
import com.shin.streaming.dto.WatchVodResult;

import java.util.UUID;

public interface VodService {

    WatchVodResult watchVod(UUID userId, UUID videoId, String videoUrl);
    void handlePlaybackEvent(ViewEventRequest request, UUID userId);
}
