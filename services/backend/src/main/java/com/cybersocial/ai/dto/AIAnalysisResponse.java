package com.cybersocial.ai.dto;

import com.cybersocial.ai.RiskLevel;

public record AIAnalysisResponse(
        double fakeProbability,
        String explanation,
        RiskLevel riskLevel
) {
}
