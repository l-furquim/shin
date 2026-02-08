package com.shin.upload.service;

import com.shin.upload.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    RawUploadResponse uploadRawVideo(String userId, RawUploadData data, MultipartFile file);

    InitiateUploadResponse initiateUpload(String userId, InitiateUploadRequest request);

    ChunkUploadResponse uploadChunk(String uploadId, Integer chunkNumber, Integer totalChunks, byte[] data);

    CancelUploadResponse cancelUpload(String uploadId);
}
