package com.cybersocial.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai-service")
public record AiServiceProperties(
        String url,
        String apiKey
) {
    public AiServiceProperties {
        if (url == null || url.isBlank()) {
            url = "http://localhost:8000";
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
