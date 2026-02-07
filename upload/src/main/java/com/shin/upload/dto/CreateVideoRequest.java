package com.shin.upload.dto;

import java.util.List;
import java.util.UUID;

public record CreateVideoRequest(
    UUID videoId,
    String title,
    String description,
    String visibility,
    String status,
    String accountId,
    List<String> resolutions
) {}
