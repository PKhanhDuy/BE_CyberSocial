package com.cybersocial.ai;

import com.cybersocial.ai.dto.AIAnalysisRequest;
import com.cybersocial.ai.dto.AIAnalysisResponse;

public interface AIAnalysisService {

    AIAnalysisResponse analyze(AIAnalysisRequest request);
}
