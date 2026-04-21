package com.shin.metadata.dto;


public record ProcessingDetails(
        Integer transcodingProgress,
        String transcodingStatus,
        String transcodingFailureReason
) {
}
