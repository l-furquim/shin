package com.shin.upload.dto;

import java.util.List;
import java.util.UUID;

public record ChunkedUploadResponse(UUID uploadId, UUID videoId, long totalChunks, List<PresignedChunk> chunks) {}
