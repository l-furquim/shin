package com.shin.metadata.service;

import com.shin.metadata.dto.*;

import java.util.UUID;

public interface VideoService {

    InitVideoResponse initVideo(String userId);

    CreateVideoResponse createVideo(CreateVideoRequest request);

    GetVideoByIdResponse getVideoById(UUID id);

    PatchVideoByIdResponse patchVideoById(UUID id, PatchVideoByIdRequest request);

    void deleteVideoById(UUID id);

    SearchVideosResponse searchByAccountId(String accountId, int start, int end);

    SearchVideosResponse searchByCategory(String categoryName, int start, int end);

    SearchVideosResponse searchAll(int start, int end);

    void updateVideoProcessingStatus(String videoId, String status, String[] resolutions, Double duration);

}
