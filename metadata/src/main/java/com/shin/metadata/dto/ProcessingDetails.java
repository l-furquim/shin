package com.shin.metadata.dto;

import com.shin.metadata.model.enums.ProcessingStatus;

public record ProcessingDetails(
        ProcessingStatus processingStatus,
        String processingFailureReason,
        ProcessingProgress processingProgress
) {
}
