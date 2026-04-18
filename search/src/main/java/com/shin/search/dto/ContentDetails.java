package com.shin.search.dto;

public record ContentDetails(
        String resolutions,
        Double duration,
        String processedPath,
        String defaultLanguage,
        String publishedLocale,
        Boolean onlyForAdults
) {
}
