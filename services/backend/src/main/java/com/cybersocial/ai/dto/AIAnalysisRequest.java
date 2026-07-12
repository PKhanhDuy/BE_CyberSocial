package com.cybersocial.ai.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AIAnalysisRequest(
        @Size(max = 10000, message = "Text must be 10000 characters or fewer")
        String text,
        UUID postId,
        Boolean includeXai
) {
    public AIAnalysisRequest {
        if (includeXai == null) {
            includeXai = true;
        }
    }

    @AssertTrue(message = "Text is required when postId is not provided")
    public boolean isTextPresentWhenNeeded() {
        if (postId != null) {
            return true;
        }
        return text != null && !text.isBlank();
    }
}
