package com.shin.metadata.dto;

import com.shin.metadata.model.enums.VideoLanguage;

public record ContentDetails(
        String resolutions,
        Double duration,
        String processedPath,
        VideoLanguage defaultLanguage,
        String publishedLocale,
        Boolean onlyForAdults
) {
}
