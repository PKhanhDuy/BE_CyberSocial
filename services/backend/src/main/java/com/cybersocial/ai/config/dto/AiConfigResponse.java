package com.cybersocial.ai.config.dto;

import java.time.Instant;
import java.util.List;

public record AiConfigResponse(
        boolean enabled,
        List<Integer> tierThresholds,
        int debounceMinutes,
        int fakeThresholdPercent,
        int maxTiers,
        int timeoutSeconds,
        boolean includeSyntheticPosts,
        Instant updatedAt
) {
}
