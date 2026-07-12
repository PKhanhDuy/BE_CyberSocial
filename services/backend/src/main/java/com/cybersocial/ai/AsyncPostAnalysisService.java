package com.cybersocial.ai;

import com.cybersocial.ai.dto.AIAnalysisRequest;
import com.cybersocial.ai.dto.AIAnalysisResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncPostAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AsyncPostAnalysisService.class);

    private final AIAnalysisService aiAnalysisService;
    private final PostVerificationWriter postVerificationWriter;
    private final PostAnalysisTriggerService postAnalysisTriggerService;

    public AsyncPostAnalysisService(
            AIAnalysisService aiAnalysisService,
            PostVerificationWriter postVerificationWriter,
            @Lazy PostAnalysisTriggerService postAnalysisTriggerService
    ) {
        this.aiAnalysisService = aiAnalysisService;
        this.postVerificationWriter = postVerificationWriter;
        this.postAnalysisTriggerService = postAnalysisTriggerService;
    }

    @Async("postAnalysisExecutor")
    public void analyzePost(UUID postId, int targetTier, int interactionCount) {
        try {
            postVerificationWriter.markAnalyzing(postId, interactionCount);
            AIAnalysisResponse response = aiAnalysisService.analyze(new AIAnalysisRequest(
                    "",
                    postId,
                    true
            ));
            postVerificationWriter.saveSuccess(postId, targetTier, interactionCount, response);
            log.info(
                    "Auto-analysis completed for post {} tier {} mode {} label {}",
                    postId,
                    targetTier,
                    response.mode(),
                    response.label()
            );
        } catch (Exception exception) {
            postVerificationWriter.markFailed(postId, exception.getMessage());
            log.warn("Auto-analysis failed for post {} tier {}", postId, targetTier, exception);
        } finally {
            postAnalysisTriggerService.recheck(postId);
        }
    }
}
