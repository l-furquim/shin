package com.shin.comment.infrastructure.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiFieldError", description = "Validation or constraint violation item")
public record ApiFieldError(
        @Schema(description = "Field path that failed validation", example = "content")
        String field,
        @Schema(description = "Error message for the field", example = "must not be blank")
        String message
) {
}
