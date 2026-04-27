package com.shin.metadata.dto;

public record FileDetails(
       String fileName,
       Long fileSize,
       String fileType
) {
}
