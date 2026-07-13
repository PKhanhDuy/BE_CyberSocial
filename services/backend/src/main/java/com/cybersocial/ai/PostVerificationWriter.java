package com.cybersocial.ai;

import com.cybersocial.ai.dto.AIAnalysisResponse;
import com.cybersocial.notification.NotificationService;
import com.cybersocial.notification.NotificationType;
import com.cybersocial.post.Post;
import com.cybersocial.post.PostRepository;
import com.cybersocial.post.PostVerification;
import com.cybersocial.post.PostVerificationRepository;
import com.cybersocial.post.PostVerificationService;
import com.cybersocial.post.PostVerificationStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostVerificationWriter {

    private final PostRepository postRepository;
    private final PostVerificationRepository postVerificationRepository;
    private final PostVerificationService postVerificationService;
    private final NotificationService notificationService;

    public PostVerificationWriter(
            PostRepository postRepository,
            PostVerificationRepository postVerificationRepository,
            PostVerificationService postVerificationService,
            NotificationService notificationService
    ) {
        this.postRepository = postRepository;
        this.postVerificationRepository = postVerificationRepository;
        this.postVerificationService = postVerificationService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void markAnalyzing(UUID postId, int interactionCount) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new IllegalStateException("Post not found: " + postId));
        PostVerification verification = postVerificationService.getOrCreate(post);
        verification.setVerificationStatus(PostVerificationStatus.ANALYZING);
        verification.setInteractionCountAtAnalysis(interactionCount);
        postVerificationRepository.save(verification);
    }

    @Transactional
    public void saveSuccess(UUID postId, int targetTier, int interactionCount, AIAnalysisResponse response) {
        PostVerification verification = postVerificationRepository.findByPostId(postId)
                .orElseThrow(() -> new IllegalStateException("Verification not found for post: " + postId));

        String previousLabel = verification.getLabel();

        verification.setVerificationStatus(PostVerificationStatus.COMPLETED);
        verification.setAnalysisTier(targetTier);
        verification.setInteractionCountAtAnalysis(interactionCount);
        verification.setLastAnalyzedAt(Instant.now());
        verification.setFakeProbability(toDecimal(response.fakeProbability()));
        verification.setConfidenceScore(toConfidenceScore(response.fakeProbability()));
        verification.setLabel(response.label());
        verification.setRiskLevel(response.riskLevel());
        verification.setThreshold(response.threshold() == null ? null : toDecimal(response.threshold()));
        verification.setMode(response.mode());
        verification.setInterpretation(response.explanation());
        verification.setEventAttributions(
                response.eventAttributions() == null ? List.of() : response.eventAttributions()
        );
        verification.setPropagationTimeline(
                response.propagationTimeline() == null ? List.of() : response.propagationTimeline()
        );
        postVerificationRepository.save(verification);

        if ("FAKE".equals(response.label()) && !"FAKE".equals(previousLabel)) {
            notifyAuthorOfFakeLabel(postId, response);
        }
    }

    private void notifyAuthorOfFakeLabel(UUID postId, AIAnalysisResponse response) {
        postRepository.findByIdWithAuthor(postId).ifPresent(post -> {
            if (post.isSynthetic()) {
                return;
            }

            String excerpt = truncate(post.getContent(), 120);
            String riskText = response.riskLevel() == null ? "" : " Mức rủi ro: " + response.riskLevel() + ".";

            notificationService.create(
                    post.getAuthor(),
                    NotificationType.SECURITY,
                    "Cảnh báo: Bài viết được gắn nhãn tin giả",
                    "Hệ thống AI đã phân tích và xác định bài viết của bạn có khả năng là tin giả."
                            + riskText
                            + (excerpt.isBlank() ? "" : " Nội dung: \"" + excerpt + "\"")
            );
        });
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 3) + "...";
    }

    @Transactional
    public void markFailed(UUID postId, String message) {
        postVerificationRepository.findByPostId(postId).ifPresent(verification -> {
            verification.setVerificationStatus(PostVerificationStatus.FAILED);
            verification.setInterpretation(message);
            verification.setLastAnalyzedAt(Instant.now());
            postVerificationRepository.save(verification);
        });
    }

    private BigDecimal toDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal toConfidenceScore(double fakeProbability) {
        return BigDecimal.valueOf(fakeProbability * 100.0).setScale(2, RoundingMode.HALF_UP);
    }
}
