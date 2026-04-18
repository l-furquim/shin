package com.shin.metadata.dto;

import com.shin.metadata.model.enums.TranscodingStatus;

public record ProcessingDetails(
        TranscodingStatus transcodingStatus,
        String processingFailureReason,
        ProcessingProgress processingProgress
) {
}
