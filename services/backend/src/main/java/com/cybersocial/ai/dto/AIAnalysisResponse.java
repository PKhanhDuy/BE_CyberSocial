package com.cybersocial.ai.dto;

import com.cybersocial.ai.RiskLevel;
import java.util.List;

public record AIAnalysisResponse(
        double fakeProbability,
        String explanation,
        RiskLevel riskLevel,
        String label,
        Double threshold,
        Integer eventCount,
        String mode,
        Double graphContribution,
        List<EventAttributionResponse> eventAttributions,
        List<PropagationTimelineEventResponse> propagationTimeline
) {
    public AIAnalysisResponse(double fakeProbability, String explanation, RiskLevel riskLevel) {
        this(fakeProbability, explanation, riskLevel, null, null, null, null, null, List.of(), List.of());
    }

    public AIAnalysisResponse(
            double fakeProbability,
            String explanation,
            RiskLevel riskLevel,
            String label,
            Double threshold,
            Integer eventCount,
            String mode,
            Double graphContribution
    ) {
        this(
                fakeProbability,
                explanation,
                riskLevel,
                label,
                threshold,
                eventCount,
                mode,
                graphContribution,
                List.of(),
                List.of()
        );
    }

    public AIAnalysisResponse(
            double fakeProbability,
            String explanation,
            RiskLevel riskLevel,
            String label,
            Double threshold,
            Integer eventCount,
            String mode,
            Double graphContribution,
            List<EventAttributionResponse> eventAttributions
    ) {
        this(
                fakeProbability,
                explanation,
                riskLevel,
                label,
                threshold,
                eventCount,
                mode,
                graphContribution,
                eventAttributions,
                List.of()
        );
    }
}
