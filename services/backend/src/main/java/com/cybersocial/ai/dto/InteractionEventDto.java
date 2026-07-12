package com.cybersocial.ai.dto;

public record InteractionEventDto(
        String eventId,
        String parentEventId,
        long srcUserId,
        long dstArticleId,
        Double timestampUnix,
        Double eventOrder,
        String eventType,
        double retweetCount,
        double favoriteCount,
        Double textLen,
        String actorDisplayName,
        UserProfileFeaturesDto userProfile,
        TreeFeaturesDto tree
) {
}
