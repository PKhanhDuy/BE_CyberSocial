package com.cybersocial.ai.dto;

public record AIMonitoringStatsResponse(
        double averageTrustScore,
        double fakeDetectionRate,
        long fakePostCount,
        long totalPostCount,
        long verifiedPostCount
) {
}
