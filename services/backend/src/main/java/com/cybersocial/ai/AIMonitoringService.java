package com.cybersocial.ai;

import com.cybersocial.ai.dto.AIMonitoringStatsResponse;
import com.cybersocial.ai.dto.PendingScanPostResponse;
import com.cybersocial.post.Post;
import com.cybersocial.post.PostRepository;
import com.cybersocial.post.PostVerification;
import com.cybersocial.post.PostVerificationRepository;
import com.cybersocial.post.PostVerificationService;
import com.cybersocial.post.PostVerificationStatus;
import com.cybersocial.common.util.SecurityUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AIMonitoringService {

    private static final int DEFAULT_PENDING_LIMIT = 3;
    private static final int PREVIEW_MAX_LENGTH = 96;
    private static final int CANDIDATE_POST_LIMIT = 100;

    private final PostVerificationRepository postVerificationRepository;
    private final PostRepository postRepository;
    private final PostVerificationService postVerificationService;

    public AIMonitoringService(
            PostVerificationRepository postVerificationRepository,
            PostRepository postRepository,
            PostVerificationService postVerificationService
    ) {
        this.postVerificationRepository = postVerificationRepository;
        this.postRepository = postRepository;
        this.postVerificationService = postVerificationService;
    }

    @Transactional(readOnly = true)
    public AIMonitoringStatsResponse getStats() {
        long totalPostCount = postRepository.countVisiblePosts();
        long fakePostCount = postVerificationRepository.countCompletedFakeVerifications();
        long verifiedPostCount = postVerificationRepository.countCompletedVerifications();
        Double averageRealProbability = postVerificationRepository.averageRealProbability();

        double averageTrustScore = averageRealProbability == null
                ? 0.0
                : roundTwoDecimals(averageRealProbability * 100.0);
        double fakeDetectionRate = totalPostCount == 0
                ? 0.0
                : roundTwoDecimals((fakePostCount * 100.0) / totalPostCount);

        return new AIMonitoringStatsResponse(
                averageTrustScore,
                fakeDetectionRate,
                fakePostCount,
                totalPostCount,
                verifiedPostCount
        );
    }

    @Transactional(readOnly = true)
    public List<PendingScanPostResponse> getPendingScanPosts(int limit) {
        int resolvedLimit = Math.min(Math.max(limit, 1), 10);
        List<Post> posts = postRepository.findVisiblePosts(
                SecurityUtils.requireCurrentUserId(),
                PageRequest.of(0, CANDIDATE_POST_LIMIT)
        ).getContent();
        if (posts.isEmpty()) {
            return List.of();
        }

        List<UUID> postIds = posts.stream().map(Post::getId).toList();
        Map<UUID, PostVerification> verificationByPostId = postVerificationRepository.findByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(verification -> verification.getPost().getId(), Function.identity()));

        return posts.stream()
                .filter(post -> postVerificationService.isAwaitingInteractionThreshold(
                        post.getId(),
                        verificationByPostId.get(post.getId())
                ))
                .map(post -> toPendingScanPost(post, verificationByPostId.get(post.getId())))
                .sorted(Comparator
                        .comparingLong(PendingScanPostResponse::totalInteractions).reversed()
                        .thenComparing(PendingScanPostResponse::postId))
                .limit(resolvedLimit)
                .toList();
    }

    public List<PendingScanPostResponse> getPendingScanPosts() {
        return getPendingScanPosts(DEFAULT_PENDING_LIMIT);
    }

    private PendingScanPostResponse toPendingScanPost(Post post, PostVerification verification) {
        int completedTier = verification == null ? 0 : verification.getAnalysisTier();
        long totalInteractions = postVerificationService.totalInteractions(post.getId());
        int nextThreshold = postVerificationService.nextThresholdFor(totalInteractions, completedTier);

        return new PendingScanPostResponse(
                post.getId(),
                toNodeId(post.getId()),
                post.getAuthor().getDisplayName(),
                previewContent(post.getContent()),
                PostVerificationStatus.PENDING,
                totalInteractions,
                nextThreshold
        );
    }

    private static String previewContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_MAX_LENGTH - 3) + "...";
    }

    private static String toNodeId(UUID postId) {
        String compact = postId.toString().replace("-", "");
        return "0x" + compact.substring(0, 4).toUpperCase();
    }

    private static double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
