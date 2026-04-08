package com.shin.upload.dto;

import java.util.List;
import java.util.UUID;

public record ChunkedUploadResponse(UUID videoId, List<PresignedChunk> chunks) {}
