package com.cybersocial.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventAttributionResponse(
        int eventIndex,
        String eventType,
        String eventTypeLabel,
        String relativeTime,
        String actorLabel,
        Double tigeRemoval,
        Double confidenceDrop,
        Double conditionalTige,
        String summary,
        String impactLevel
) {
}
