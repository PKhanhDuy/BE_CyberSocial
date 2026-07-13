package com.cybersocial.ai;

import com.cybersocial.ai.config.AiRuntimeConfigService;
import com.cybersocial.post.Post;
import com.cybersocial.post.PostRepository;
import com.cybersocial.post.PostVerificationService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PostAnalysisTriggerService {

    private static final Logger log = LoggerFactory.getLogger(PostAnalysisTriggerService.class);

    private final AiAnalysisProperties properties;
    private final AiRuntimeConfigService runtimeConfig;
    private final PostRepository postRepository;
    private final PostVerificationService postVerificationService;
    private final AsyncPostAnalysisService asyncPostAnalysisService;

    public PostAnalysisTriggerService(
            AiAnalysisProperties properties,
            AiRuntimeConfigService runtimeConfig,
            PostRepository postRepository,
            PostVerificationService postVerificationService,
            @Lazy AsyncPostAnalysisService asyncPostAnalysisService
    ) {
        this.properties = properties;
        this.runtimeConfig = runtimeConfig;
        this.postRepository = postRepository;
        this.postVerificationService = postVerificationService;
        this.asyncPostAnalysisService = asyncPostAnalysisService;
    }

    public void onInteraction(UUID postId) {
        scheduleCheck(postId, true);
    }

    public void afterPropagationSimulation(UUID postId) {
        scheduleCheck(postId, true);
    }

    public void recheck(UUID postId) {
        scheduleCheck(postId, false);
    }

    private void scheduleCheck(UUID postId, boolean enforceDebounce) {
        if (!runtimeConfig.enabled()) {
            return;
        }

        Runnable task = () -> {
            try {
                tryTrigger(postId, enforceDebounce);
            } catch (Exception exception) {
                log.warn("Failed to evaluate auto-analysis trigger for post {}", postId, exception);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }

        task.run();
    }

    void tryTrigger(UUID postId, boolean enforceDebounce) {
        if (!runtimeConfig.enabled()) {
            return;
        }

        Post post = postRepository.findByIdWithAuthor(postId).orElse(null);
        if (post == null) {
            return;
        }
        if (post.isSynthetic() && !properties.includeSyntheticPosts()) {
            return;
        }

        long totalInteractions = postVerificationService.totalInteractions(postId);
        var verification = postVerificationService.getOrCreate(post);

        if (verification.getVerificationStatus() == com.cybersocial.post.PostVerificationStatus.ANALYZING) {
            return;
        }

        List<Integer> thresholds = runtimeConfig.thresholds();
        int nextTier = verification.getAnalysisTier() + 1;
        if (nextTier > properties.maxTiers() || nextTier > thresholds.size()) {
            return;
        }

        int requiredInteractions = thresholds.get(nextTier - 1);
        if (totalInteractions < requiredInteractions) {
            return;
        }

        if (enforceDebounce && verification.getLastAnalyzedAt() != null) {
            long elapsedMinutes = java.time.Duration.between(
                    verification.getLastAnalyzedAt(),
                    java.time.Instant.now()
            ).toMinutes();
            if (elapsedMinutes < runtimeConfig.debounceMinutes()) {
                return;
            }
        }

        asyncPostAnalysisService.analyzePost(postId, nextTier, (int) totalInteractions);
    }
}
