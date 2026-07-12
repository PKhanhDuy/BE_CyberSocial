package com.cybersocial.ai.dto;

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
        boolean influential
) {
}
