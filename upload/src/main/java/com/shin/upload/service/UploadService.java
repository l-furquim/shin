package com.shin.upload.service;

import com.shin.upload.dto.*;


public interface UploadService {

    RawUploadResponse uploadRawVideo(String userId, RawUploadData data);

    InitiateUploadResponse initiateUpload(String userId, InitiateUploadRequest request);

    ChunkUploadResponse uploadChunk(String uploadId, Integer chunkNumber, byte[] data);

    RawUploadResponse completeUpload(String uploadId);

    CancelUploadResponse cancelUpload(String uploadId);
}
