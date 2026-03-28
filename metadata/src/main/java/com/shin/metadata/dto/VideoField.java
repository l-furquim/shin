package com.shin.metadata.dto;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public enum VideoField {

    THUMBNAILS("thumbnails"),
    CONTENT_DETAILS("contentDetails"),
    STATISTICS("statistics"),
    FILE_DETAILS("fileDetails"),
    PROCESSING_DETAILS("processingDetails"),
    CHANNEL("channel"),
    TAGS("tags");

    private final String key;

    VideoField(String key) {
        this.key = key;
    }

    public static Optional<VideoField> fromKey(String key) {
        return Arrays.stream(values())
                .filter(f -> f.key.equals(key))
                .findFirst();
    }

    public static Set<VideoField> parse(String fields) {
        if (fields == null || fields.isBlank()) {
            return EnumSet.allOf(VideoField.class);
        }
        Set<VideoField> result = EnumSet.noneOf(VideoField.class);
        for (String key : fields.split(",")) {
            fromKey(key.trim()).ifPresent(result::add);
        }
        return result;
    }
}
