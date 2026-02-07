package com.shin.upload.service;

import com.shin.upload.dto.CancelUploadResponse;
import com.shin.upload.dto.ChunkUploadResponse;
import com.shin.upload.dto.InitiateUploadRequest;
import com.shin.upload.dto.InitiateUploadResponse;

public interface UploadService {

    InitiateUploadResponse initiateUpload(InitiateUploadRequest request);

    ChunkUploadResponse uploadChunk(String uploadId, Integer chunkNumber, Integer totalChunks, byte[] data);

    CancelUploadResponse cancelUpload(String uploadId);
}
