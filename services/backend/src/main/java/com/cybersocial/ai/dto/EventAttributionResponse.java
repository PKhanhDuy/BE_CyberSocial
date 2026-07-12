package com.cybersocial.ai.dto;

public record EventAttributionResponse(
        int eventIndex,
        String eventType,
        String eventTypeLabel,
        String relativeTime,
        String actorLabel,
        Double tigeRemoval,
        Double confidenceDrop,
        String summary
) {
}
