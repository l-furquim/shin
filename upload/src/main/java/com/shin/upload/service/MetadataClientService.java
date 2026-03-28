package com.shin.upload.service;

import com.shin.upload.dto.CreateVideoRequest;
import com.shin.upload.dto.CreateVideoResponse;
import com.shin.upload.dto.UpdateVideoRequest;

public interface MetadataClientService {

    CreateVideoResponse createVideo(CreateVideoRequest request);

    void updateVideo(UpdateVideoRequest request, String videoId);
}
