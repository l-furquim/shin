package com.shin.metadata.dto;


import java.util.UUID;

public record EncodingProgressEvent(
       UUID videoId,
       Integer progress,
       Long timeProcessingSeconds,
       String failure
) {
}
