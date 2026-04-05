package com.shin.interaction.model;

import java.util.Arrays;
import java.util.Optional;

public enum REACTION_TYPE {

    LIKE("like"),
    DESLIKE("dislike");

    private final String key;

    REACTION_TYPE(String key) {
        this.key = key;
    }

    public static Optional<REACTION_TYPE> fromKey(String key) {
        return Arrays.stream(values())
                .filter(e -> e.key.equals(key))
                .findFirst();
    }
}
