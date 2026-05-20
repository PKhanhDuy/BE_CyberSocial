package com.cybersocial.upload;

public record ImageUploadResponse(
        String publicId,
        String originalFileName,
        String contentType,
        long size,
        String url
) {
}
