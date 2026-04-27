package com.shin.metadata.dto;

public record ProcessingProgress(
        Integer partsTotal,
        Integer partsProcessed
) {
}
