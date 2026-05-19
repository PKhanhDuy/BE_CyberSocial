package com.cybersocial.ai;

import com.cybersocial.ai.dto.AIAnalysisRequest;
import com.cybersocial.ai.dto.AIAnalysisResponse;
import com.cybersocial.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AIAnalysisController {

    private final AIAnalysisService analysisService;

    public AIAnalysisController(AIAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<AIAnalysisResponse>> analyze(@Valid @RequestBody AIAnalysisRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Analysis completed", analysisService.analyze(request)));
    }
}
