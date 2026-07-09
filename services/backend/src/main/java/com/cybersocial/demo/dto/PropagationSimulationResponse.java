package com.cybersocial.demo.dto;

import com.cybersocial.demo.PropagationPattern;
import java.time.Instant;
import java.util.UUID;

public record PropagationSimulationResponse(
        UUID postId,
        PropagationPattern pattern,
        int demoUsersAvailable,
        int usersCreated,
        int likesCreated,
        int commentsCreated,
        int sharesCreated,
        long totalLikes,
        long totalComments,
        long totalShares,
        int durationSeconds,
        Instant startedAt,
        Instant endedAt
) {
}
