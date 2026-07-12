package com.cybersocial.ai;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai-analysis")
public record AiAnalysisProperties(
        boolean enabled,
        List<Integer> thresholds,
        int debounceMinutes,
        int maxTiers,
        boolean includeSyntheticPosts,
        int timeoutSeconds
) {
    public AiAnalysisProperties {
        if (thresholds == null || thresholds.isEmpty()) {
            thresholds = List.of(5, 15, 30);
        }
        if (debounceMinutes <= 0) {
            debounceMinutes = 3;
        }
        if (maxTiers <= 0) {
            maxTiers = 3;
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 30;
        }
    }
}
