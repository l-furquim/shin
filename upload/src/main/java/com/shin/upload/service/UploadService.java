package com.shin.upload.service;

import com.shin.upload.dto.*;


public interface UploadService {

    RawUploadResponse initiateRawUpload(String userId, RawUploadData data);

    ChunkedUploadResponse initiateUpload(String userId, ChunkedUploadRequest request);

    void completeUpload(String uploadId);

    void cancelUpload(String uploadId);
}
