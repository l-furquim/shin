package com.shin.upload.dto;

import java.util.List;

public record TranscodeJobEvent(
    String videoId,
    String s3Key,
    String userId,
    String fileName,
    List<String> resolutions
) {}
