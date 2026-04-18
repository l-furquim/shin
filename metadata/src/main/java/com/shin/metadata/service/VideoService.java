package com.shin.metadata.service;

import com.shin.metadata.dto.*;

import java.util.Set;
import java.util.UUID;

public interface VideoService {

    CreateVideoResponse createVideo(CreateVideoRequest request);

    VideoDto getVideoById(UUID id, UUID userId, Set<VideoField> fields);

    VideoDto patchVideoById(UUID id, PatchVideoByIdRequest request);

    void deleteVideoById(UUID id);

    void updateVideoProcessingStatus(String videoId, String status, String processedPath, String[] resolutions, Long duration, String fileName, Long fileSize, String fileType);

    void updateVideoThumbnail(String videoId, String thumbnailUrl);

    SearchVideosResponse search(SearchVideosRequest request, UUID userId, Set<VideoField> fields);

    void increaseVideoView(UUID videoId, String viewerKey);

    WatchVideoResponse getWatchVideoById(UUID id);

    void publish(UUID id, UUID userId);

}
