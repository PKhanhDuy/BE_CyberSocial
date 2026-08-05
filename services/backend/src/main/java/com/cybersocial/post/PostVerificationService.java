package com.cybersocial.post;

import com.cybersocial.ai.config.AiRuntimeConfigService;
import com.cybersocial.common.exception.ForbiddenOperationException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.post.dto.PostVerificationResponse;
import com.cybersocial.post.dto.VerifiedNewsStatsResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostVerificationService {

    private final PostVerificationRepository postVerificationRepository;
    private final PostRepository postRepository;
    private final PostStatisticsService postStatisticsService;
    private final AiRuntimeConfigService aiRuntimeConfigService;

    public PostVerificationService(
            PostVerificationRepository postVerificationRepository,
            PostRepository postRepository,
            PostStatisticsService postStatisticsService,
            AiRuntimeConfigService aiRuntimeConfigService
    ) {
        this.postVerificationRepository = postVerificationRepository;
        this.postRepository = postRepository;
        this.postStatisticsService = postStatisticsService;
        this.aiRuntimeConfigService = aiRuntimeConfigService;
    }

    @Transactional(readOnly = true)
    public PostVerificationResponse findByPostId(UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found");
        }

        long totalInteractions = totalInteractions(postId);
        int nextThreshold = nextThresholdFor(totalInteractions, 0);

        return postVerificationRepository.findByPostId(postId)
                .map(verification -> PostVerificationResponse.from(
                        verification,
                        totalInteractions,
                        nextThresholdFor(totalInteractions, verification.getAnalysisTier())
                ))
                .orElseGet(() -> PostVerificationResponse.pending(postId, totalInteractions, nextThreshold));
    }

    @Transactional
    public PostVerification getOrCreate(Post post) {
        return postVerificationRepository.findByPostId(post.getId())
                .orElseGet(() -> postVerificationRepository.save(PostVerification.builder()
                        .post(post)
                        .verificationStatus(PostVerificationStatus.PENDING)
                        .analysisTier(0)
                        .build()));
    }

    @Transactional
    public PostVerification initializeForPost(Post post) {
        return getOrCreate(post);
    }

    @Transactional(readOnly = true)
    public boolean isAwaitingInteractionThreshold(UUID postId, PostVerification verification) {
        if (verification != null && verification.getVerificationStatus() == PostVerificationStatus.ANALYZING) {
            return false;
        }

        int completedTier = verification == null ? 0 : verification.getAnalysisTier();
        long totalInteractions = totalInteractions(postId);
        int nextThreshold = nextThresholdFor(totalInteractions, completedTier);
        return nextThreshold > 0 && totalInteractions < nextThreshold;
    }

    public long totalInteractions(UUID postId) {
        PostStatistics statistics = postStatisticsService.find(postId);
        return statistics.likeCount() + statistics.commentCount() + statistics.shareCount();
    }

    public int nextThresholdFor(long totalInteractions, int completedTier) {
        List<Integer> thresholds = aiRuntimeConfigService.thresholds();
        for (int index = completedTier; index < thresholds.size(); index++) {
            int threshold = thresholds.get(index);
            if (totalInteractions < threshold) {
                return threshold;
            }
        }
        return 0;
    }

    @Transactional(readOnly = true)
    public void assertInteractionsAllowed(Post post) {
        Post contentRoot = resolveContentRoot(post);
        postVerificationRepository.findByPostId(contentRoot.getId())
                .filter(PostVerification::interactionsLocked)
                .ifPresent(ignored -> {
                    throw new ForbiddenOperationException(
                            "Bài viết này đã bị gắn nhãn tin giả và không thể tương tác.");
                });
    }

    private Post resolveContentRoot(Post post) {
        Post current = post;
        while (current.getSharedPost() != null) {
            UUID parentId = current.getSharedPost().getId();
            current = postRepository.findByIdWithSharedPost(parentId)
                    .orElse(current.getSharedPost());
        }
        return current;
    }

    @Transactional(readOnly = true)
    public VerifiedNewsStatsResponse findVerifiedNewsStats() {
        return new VerifiedNewsStatsResponse(
                postVerificationRepository.countVerifiedRealPosts(),
                postVerificationRepository.averageVerifiedAnalysisDelayMs()
        );
    }
}
