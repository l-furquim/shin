package com.shin.metadata.dto;

import java.util.Arrays;
import java.util.Optional;

public enum MyRating {

    LIKE("like"),
    DESLIKE("deslike");

    private final String key;

    MyRating(String key) {
        this.key = key;
    }

    public static Optional<MyRating> fromKey(String key) {
        return Arrays.stream(values())
                .filter(f -> f.key.equals(key))
                .findFirst();
    }

}
