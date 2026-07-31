package com.cybersocial.common.response;

import java.util.Map;

public record ErrorResponse(
        String code,
        String path,
        Map<String, String> validationErrors,
        String reason
) {
    public static ErrorResponse of(String code, String path) {
        return new ErrorResponse(code, path, null, null);
    }

    public static ErrorResponse of(String code, String path, String reason) {
        return new ErrorResponse(code, path, null, reason);
    }

    public static ErrorResponse validation(String path, Map<String, String> validationErrors) {
        return new ErrorResponse("VALIDATION_ERROR", path, validationErrors, null);
    }
}
