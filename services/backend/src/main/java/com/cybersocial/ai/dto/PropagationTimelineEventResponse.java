package com.cybersocial.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PropagationTimelineEventResponse(
        int eventIndex,
        String eventId,
        String parentEventId,
        int depth,
        String relativeTime,
        String eventType,
        String eventTypeLabel,
        String actorLabel,
        Double tigeRemoval,
        Double conditionalTige,
        boolean influential
) {
}
