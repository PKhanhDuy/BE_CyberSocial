package com.cybersocial.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AIAnalysisRequest(
        @NotBlank(message = "Text is required")
        @Size(max = 10000, message = "Text must be 10000 characters or fewer")
        String text
) {
}
