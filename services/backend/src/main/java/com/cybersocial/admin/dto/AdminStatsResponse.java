package com.cybersocial.admin.dto;

import com.cybersocial.ai.dto.AIMonitoringStatsResponse;

public record AdminStatsResponse(
        long totalUsers,
        long activeUsers,
        long lockedUsers,
        long totalPosts,
        long visiblePosts,
        long hiddenPosts,
        long acceptedFriendships,
        long totalFollows,
        long totalMessages,
        long activeStories,
        AIMonitoringStatsResponse aiStats
) {
}
