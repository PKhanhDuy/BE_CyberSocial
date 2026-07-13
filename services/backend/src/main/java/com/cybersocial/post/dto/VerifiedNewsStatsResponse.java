package com.cybersocial.post.dto;

public record VerifiedNewsStatsResponse(
        long verifiedPostCount,
        Double averageAnalysisDelayMs
) {
}
