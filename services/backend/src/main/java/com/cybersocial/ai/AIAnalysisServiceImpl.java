package com.cybersocial.ai;

import com.cybersocial.ai.dto.AIAnalysisRequest;
import com.cybersocial.ai.dto.AIAnalysisResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AIAnalysisServiceImpl implements AIAnalysisService {

    private final RestClient restClient;

    public AIAnalysisServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${app.ai-service.url:${AI_SERVICE_URL:http://localhost:8000}}") String aiServiceUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(aiServiceUrl).build();
    }

    @Override
    public AIAnalysisResponse analyze(AIAnalysisRequest request) {
        AIServiceResponse response = restClient.post()
                .uri("/analyze")
                .body(new AIServiceRequest(request.text(), true))
                .retrieve()
                .body(AIServiceResponse.class);

        if (response == null) {
            throw new IllegalStateException("AI service returned an empty response");
        }

        return new AIAnalysisResponse(
                response.fakeProbability(),
                response.explanation(),
                response.riskLevel()
        );
    }

    private record AIServiceRequest(String text, boolean includeXai) {
    }

    private record AIServiceResponse(double fakeProbability, String explanation, RiskLevel riskLevel) {
    }
}
