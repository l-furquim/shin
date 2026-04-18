package com.shin.search.dto;

public record FileDetails(
        String fileName,
        Long fileSize,
        String fileType
) {
}
