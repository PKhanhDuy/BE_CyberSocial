package com.cybersocial.ai.propagation;

public record UserSocialStats(
        double logFollowers,
        double logFollowing,
        double logStatuses,
        double accountCreatedUnix,
        double hasProfile
) {
}
