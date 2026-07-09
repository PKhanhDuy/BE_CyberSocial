package com.cybersocial.common.response;

import java.util.Map;

public record ErrorResponse(
        String code,
        String path,
        Map<String, String> validationErrors
) {
    public static ErrorResponse of(String code, String path) {
        return new ErrorResponse(code, path, null);
    }

    public static ErrorResponse validation(String path, Map<String, String> validationErrors) {
        return new ErrorResponse("VALIDATION_ERROR", path, validationErrors);
    }
}
