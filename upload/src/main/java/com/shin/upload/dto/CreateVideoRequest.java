package com.shin.upload.dto;

public record CreateVideoRequest(
    String title,
    String description,
    String visibility,
    String status,
    String accountId,
    String resolutions
) {}
