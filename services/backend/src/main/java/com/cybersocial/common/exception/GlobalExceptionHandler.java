package com.cybersocial.common.exception;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Validation failed", ErrorResponse.validation(request.getRequestURI(), errors)));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAccountLocked(
            AccountLockedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        exception.getMessage(),
                        ErrorResponse.of("ACCOUNT_LOCKED", request.getRequestURI(), exception.getReason())
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), "RESOURCE_NOT_FOUND", request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConflict(
            ConflictException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), "CONFLICT", request);
    }

    @ExceptionHandler({BadRequestException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBadRequest(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), "BAD_REQUEST", request);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMissingCookie(
            MissingRequestCookieException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "Required cookie is missing: " + exception.getCookieName(), "MISSING_COOKIE", request);
    }

    @ExceptionHandler({MultipartException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMultipart(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "Invalid upload request", "INVALID_UPLOAD", request);
    }

    @ExceptionHandler({ForbiddenOperationException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleForbidden(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), "FORBIDDEN", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUnexpected(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", "INTERNAL_SERVER_ERROR", request);
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> error(
            HttpStatus status,
            String message,
            String code,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, ErrorResponse.of(code, request.getRequestURI())));
    }
}
