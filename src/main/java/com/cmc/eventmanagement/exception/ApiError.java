package com.cmc.eventmanagement.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(int status, String message, String path, LocalDateTime timestamp,
                       @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> errors) {
    public static ApiError of(int status, String message, String path) {
        return new ApiError(status, message, path, LocalDateTime.now(), null);
    }
    public static ApiError of(int status, String message, String path, Map<String, String> errors) {
        return new ApiError(status, message, path, LocalDateTime.now(), errors);
    }
}
