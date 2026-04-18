package com.shin.metadata.dto;

import com.shin.metadata.model.enums.VideoLanguage;

public record ContentDetails(
        String resolutions,
        Long duration,
        String processedPath,
        VideoLanguage defaultLanguage,
        String publishedLocale,
        Boolean onlyForAdults
) {
}
