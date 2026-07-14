package com.cybersocial.admin.dto;

import com.cybersocial.ai.RiskLevel;
import com.cybersocial.post.Post;
import com.cybersocial.post.PostVerification;
import java.time.Instant;
import java.util.UUID;

public record AdminFakePostResponse(
        UUID postId,
        UUID authorId,
        String authorDisplayName,
        String contentPreview,
        boolean hidden,
        String label,
        Double fakeProbability,
        RiskLevel riskLevel,
        boolean publicLabel,
        String adminDecision,
        Instant reviewedAt,
        Instant lastAnalyzedAt,
        Instant createdAt
) {
    public static AdminFakePostResponse from(Post post, PostVerification verification) {
        return new AdminFakePostResponse(
                post.getId(),
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                previewContent(post.getContent()),
                post.isHidden(),
                verification.getLabel(),
                verification.getFakeProbability() == null ? null : verification.getFakeProbability().doubleValue(),
                verification.getRiskLevel(),
                verification.isPublicLabel(),
                verification.getAdminDecision(),
                verification.getReviewedAt(),
                verification.getLastAnalyzedAt(),
                post.getCreatedAt()
        );
    }

    private static String previewContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 117) + "...";
    }
}
