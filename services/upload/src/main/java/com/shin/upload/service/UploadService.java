package com.shin.upload.service;

import com.shin.upload.dto.*;

import java.util.UUID;


public interface UploadService {

    RawUploadResponse initiateRawUpload(String userId, RawUploadData data);

    ChunkedUploadResponse initiateUpload(String userId, ChunkedUploadRequest request);

    CompleteUploadResponse completeUpload(String uploadId);

    ThumbnailUploadResponse thumbnailUpload(ThumbnailUploadRequest request, UUID userId);

    void cancelUpload(String uploadId);

}
