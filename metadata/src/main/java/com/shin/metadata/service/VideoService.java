package com.shin.metadata.service;

import com.shin.metadata.dto.CreateVideoRequest;
import com.shin.metadata.dto.CreateVideoResponse;
import com.shin.metadata.dto.GetVideoByIdResponse;
import com.shin.metadata.dto.PatchVideoByIdRequest;
import com.shin.metadata.dto.PatchVideoByIdResponse;
import com.shin.metadata.dto.SearchVideosResponse;

import java.util.UUID;

public interface VideoService {

    CreateVideoResponse createVideo(CreateVideoRequest request);

    GetVideoByIdResponse getVideoById(UUID id);

    PatchVideoByIdResponse patchVideoById(UUID id, PatchVideoByIdRequest request);

    void deleteVideoById(UUID id);

    SearchVideosResponse searchByAccountId(String accountId, int start, int end);

    SearchVideosResponse searchByCategory(String categoryName, int start, int end);

    SearchVideosResponse searchAll(int start, int end);
}
