package com.cybersocial.ai;

import com.cybersocial.ai.dto.AIAnalysisRequest;
import com.cybersocial.ai.dto.AIAnalysisResponse;
import org.springframework.stereotype.Service;

@Service
public class AIAnalysisServiceImpl implements AIAnalysisService {

    @Override
    public AIAnalysisResponse analyze(AIAnalysisRequest request) {
        String normalized = request.text().toLowerCase();
        boolean suspicious = normalized.contains("urgent") || normalized.contains("guaranteed") || normalized.contains("click");
        double probability = suspicious ? 0.72 : 0.18;
        RiskLevel riskLevel = suspicious ? RiskLevel.HIGH : RiskLevel.LOW;
        String explanation = suspicious
                ? "Mock analysis flagged urgency or certainty language commonly seen in manipulative content."
                : "Mock analysis found no strong synthetic or manipulative-language signals.";
        return new AIAnalysisResponse(probability, explanation, riskLevel);
    }
}
