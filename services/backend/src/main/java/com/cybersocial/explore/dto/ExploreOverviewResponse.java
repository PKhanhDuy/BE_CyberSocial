package com.cybersocial.explore.dto;

import java.util.List;

public record ExploreOverviewResponse(
        double averageTrustScore,
        double fakeDetectionRate,
        long fakePostCount,
        long totalPostCount,
        long verifiedPostCount,
        long pendingScanCount,
        long analyzingCount,
        String loadStatus,
        int warningLevel,
        boolean syncing,
        List<String> trendingKeywords
) {
}
