package com.cybersocial.demo.dto;

import com.cybersocial.demo.PropagationPattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PropagationSimulationRequest(
        @NotNull
        UUID postId,

        @Min(1)
        @Max(300)
        Integer demoUserCount,

        @Min(0)
        @Max(300)
        Integer shares,

        @Min(0)
        @Max(300)
        Integer likes,

        @Min(0)
        @Max(300)
        Integer comments,

        @Min(1)
        @Max(3600)
        Integer durationSeconds,

        PropagationPattern pattern
) {
}
