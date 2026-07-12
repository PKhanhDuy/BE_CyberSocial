package com.cybersocial.ai.dto;

public record UserProfileFeaturesDto(
        Double logFollowers,
        Double logFollowing,
        Double logStatuses,
        Double accountCreatedUnix,
        Double hasProfile
) {
}
