package com.cybersocial.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DemoUserGenerationRequest(
        @Min(1)
        @Max(300)
        Integer count
) {
}
