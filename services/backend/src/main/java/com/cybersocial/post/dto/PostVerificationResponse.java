package com.cybersocial.post.dto;

import com.cybersocial.ai.RiskLevel;
import com.cybersocial.ai.dto.EventAttributionResponse;
import com.cybersocial.ai.dto.PropagationTimelineEventResponse;
import com.cybersocial.post.PostVerification;
import com.cybersocial.post.PostVerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostVerificationResponse(
        UUID postId,
        PostVerificationStatus status,
        Double fakeProbability,
        String label,
        RiskLevel riskLevel,
        Double threshold,
        String mode,
        int analysisTier,
        int interactionCountAtAnalysis,
        long totalInteractions,
        int nextThreshold,
        String explanation,
        String headline,
        String narrative,
        List<String> contextHints,
        List<EventAttributionResponse> eventAttributions,
        List<PropagationTimelineEventResponse> propagationTimeline,
        Instant lastAnalyzedAt,
        Instant updatedAt,
        boolean publicLabel,
        boolean interactionsLocked
) {
    public static PostVerificationResponse pending(UUID postId, long totalInteractions, int nextThreshold) {
        return new PostVerificationResponse(
                postId,
                PostVerificationStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                totalInteractions,
                nextThreshold,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                null,
                false,
                false
        );
    }

    public static PostVerificationResponse from(
            PostVerification verification,
            long totalInteractions,
            int nextThreshold
    ) {
        return new PostVerificationResponse(
                verification.getPost().getId(),
                verification.getVerificationStatus(),
                verification.getFakeProbability() == null ? null : verification.getFakeProbability().doubleValue(),
                verification.getLabel(),
                verification.getRiskLevel(),
                verification.getThreshold() == null ? null : verification.getThreshold().doubleValue(),
                verification.getMode(),
                verification.getAnalysisTier(),
                verification.getInteractionCountAtAnalysis() == null ? 0 : verification.getInteractionCountAtAnalysis(),
                totalInteractions,
                nextThreshold,
                verification.getInterpretation(),
                verification.getHeadline(),
                verification.getNarrative(),
                verification.getContextHints() == null ? List.of() : verification.getContextHints(),
                verification.getEventAttributions() == null ? List.of() : verification.getEventAttributions(),
                verification.getPropagationTimeline() == null ? List.of() : verification.getPropagationTimeline(),
                verification.getLastAnalyzedAt(),
                verification.getUpdatedAt(),
                verification.isPublicLabel(),
                verification.interactionsLocked()
        );
    }
}
