package com.shin.upload.model.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public enum UploadType {
    RAW("raw"),
    CHUNK("chunk"),
    INITIATE("initiate");

    private final String key;

    UploadType(String key) {
        this.key = key;
    }

    public static Optional<UploadType> fromKey(String key) {
        return Arrays.stream(values())
                .filter(f -> f.key.equals(key))
                .findFirst();
    }

    public static Set<UploadType> parse(String fields) {
        if (fields == null || fields.isBlank()) {
            return EnumSet.allOf(UploadType.class);
        }
        Set<UploadType> result = EnumSet.noneOf(UploadType.class);
        for (String key : fields.split(",")) {
            fromKey(key.trim()).ifPresent(result::add);
        }
        return result;
    }
}
