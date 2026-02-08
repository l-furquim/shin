package com.shin.metadata.dto;

import com.shin.metadata.validation.ValidTagIdentifier;

@ValidTagIdentifier
public record TagIdentifier(
    Integer id,
    String name
) {
    public String normalizedName() {
        return name != null ? name.trim().toLowerCase() : null;
    }
}
