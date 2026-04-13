package com.shin.comment.infrastructure.openapi.schema;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ProblemDetailResponse", description = "RFC 7807 style error payload returned by the API")
public record ProblemDetailResponse(
        @Schema(description = "URI reference that identifies the error type", example = "https://api.shin.com/errors/validation-failed")
        String type,
        @Schema(description = "Short, human-readable summary of the problem", example = "Validation Failed")
        String title,
        @Schema(description = "HTTP status code", example = "400")
        Integer status,
        @Schema(description = "Human-readable explanation specific to this occurrence", example = "Request validation failed. Please check the errors and try again.")
        String detail,
        @Schema(description = "URI reference that identifies the specific occurrence", example = "/v1/comments")
        String instance,
        @Schema(description = "Error timestamp in ISO-8601 format", example = "2026-04-11T14:28:53.740Z")
        String timestamp,
        @Schema(description = "Request correlation id for tracing", example = "e0b5f6e8-42d0-40ff-82ca-56f92dd3dd68")
        String correlationId,
        @ArraySchema(schema = @Schema(implementation = ApiFieldError.class), arraySchema = @Schema(description = "Detailed field errors for validation failures"))
        List<ApiFieldError> errors
) {
}
