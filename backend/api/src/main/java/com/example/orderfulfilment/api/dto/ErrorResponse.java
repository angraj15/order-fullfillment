package com.example.orderfulfilment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Consistent error response structure for all 4xx and 5xx responses.
 * Produced exclusively by GlobalExceptionHandler.
 */
@Schema(description = "Error response")
public record ErrorResponse(

    @Schema(description = "UTC timestamp of the error") Instant timestamp,
    @Schema(description = "HTTP status code") int status,
    @Schema(description = "HTTP status description") String error,
    @Schema(description = "Human-readable error message") String message,
    @Schema(description = "Request path that caused the error") String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }
}
