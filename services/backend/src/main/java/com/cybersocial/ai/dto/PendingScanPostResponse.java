package com.cybersocial.ai.dto;

import com.cybersocial.post.PostVerificationStatus;
import java.util.UUID;

public record PendingScanPostResponse(
        UUID postId,
        String nodeId,
        String authorDisplayName,
        String contentPreview,
        PostVerificationStatus status,
        long totalInteractions,
        int nextThreshold
) {
}
