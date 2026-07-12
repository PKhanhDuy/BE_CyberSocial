package com.cybersocial.explore;

import com.cybersocial.ai.AIMonitoringService;
import com.cybersocial.ai.dto.AIMonitoringStatsResponse;
import com.cybersocial.explore.dto.ExploreOverviewResponse;
import com.cybersocial.post.PostRepository;
import com.cybersocial.post.PostVerificationRepository;
import com.cybersocial.post.PostVerificationStatus;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExploreService {

    private static final int TRENDING_KEYWORD_LIMIT = 6;
    private static final int TRENDING_POST_SAMPLE = 120;
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([\\p{L}\\p{N}_]{2,40})");

    private final AIMonitoringService aiMonitoringService;
    private final PostVerificationRepository postVerificationRepository;
    private final PostRepository postRepository;

    public ExploreService(
            AIMonitoringService aiMonitoringService,
            PostVerificationRepository postVerificationRepository,
            PostRepository postRepository
    ) {
        this.aiMonitoringService = aiMonitoringService;
        this.postVerificationRepository = postVerificationRepository;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public ExploreOverviewResponse getOverview() {
        AIMonitoringStatsResponse aiStats = aiMonitoringService.getStats();
        long pendingScanCount = postVerificationRepository.countByStatusForVisiblePosts(PostVerificationStatus.PENDING);
        long analyzingCount = postVerificationRepository.countByStatusForVisiblePosts(PostVerificationStatus.ANALYZING);

        String loadStatus = resolveLoadStatus(aiStats.fakeDetectionRate(), pendingScanCount, analyzingCount);
        int warningLevel = resolveWarningLevel(aiStats.fakePostCount(), aiStats.fakeDetectionRate());
        boolean syncing = analyzingCount > 0 || pendingScanCount > 0;

        return new ExploreOverviewResponse(
                aiStats.averageTrustScore(),
                aiStats.fakeDetectionRate(),
                aiStats.fakePostCount(),
                aiStats.totalPostCount(),
                aiStats.verifiedPostCount(),
                pendingScanCount,
                analyzingCount,
                loadStatus,
                warningLevel,
                syncing,
                extractTrendingKeywords()
        );
    }

    private List<String> extractTrendingKeywords() {
        List<String> contents = postRepository.findRecentVisibleContents(PageRequest.of(0, TRENDING_POST_SAMPLE));
        Map<String, Integer> counts = new HashMap<>();

        for (String content : contents) {
            if (content == null || content.isBlank()) {
                continue;
            }
            Matcher matcher = HASHTAG_PATTERN.matcher(content);
            while (matcher.find()) {
                String tag = matcher.group(1);
                String normalized = tag.toLowerCase(Locale.ROOT);
                counts.merge(normalized, 1, Integer::sum);
            }
        }

        if (counts.isEmpty()) {
            return extractFallbackKeywords(contents);
        }

        return counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(TRENDING_KEYWORD_LIMIT)
                .map(entry -> "#" + entry.getKey())
                .toList();
    }

    private List<String> extractFallbackKeywords(List<String> contents) {
        Map<String, Integer> counts = new HashMap<>();
        Pattern wordPattern = Pattern.compile("[\\p{L}\\p{N}]{4,20}");

        for (String content : contents) {
            if (content == null || content.isBlank()) {
                continue;
            }
            Matcher matcher = wordPattern.matcher(content.toLowerCase(Locale.ROOT));
            while (matcher.find()) {
                String word = matcher.group();
                if (isStopWord(word)) {
                    continue;
                }
                counts.merge(word, 1, Integer::sum);
            }
        }

        return counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(TRENDING_KEYWORD_LIMIT)
                .map(entry -> "#" + entry.getKey())
                .toList();
    }

    private static boolean isStopWord(String word) {
        return List.of(
                "this", "that", "with", "from", "have", "your", "they", "will", "about", "before",
                "share", "delete", "breaking", "hello", "please", "thanks", "thank", "dang", "cho",
                "nay", "khong", "duoc", "trong", "nhung", "cac", "mot", "ban", "toi", "hay"
        ).contains(word);
    }

    private static String resolveLoadStatus(double fakeDetectionRate, long pendingScanCount, long analyzingCount) {
        if (fakeDetectionRate >= 20.0 || pendingScanCount + analyzingCount >= 20) {
            return "CRITICAL";
        }
        if (fakeDetectionRate >= 8.0 || pendingScanCount + analyzingCount >= 8) {
            return "ELEVATED";
        }
        return "STABLE";
    }

    private static int resolveWarningLevel(long fakePostCount, double fakeDetectionRate) {
        if (fakePostCount == 0 && fakeDetectionRate < 1.0) {
            return 0;
        }
        if (fakePostCount <= 2 || fakeDetectionRate < 5.0) {
            return 1;
        }
        if (fakePostCount <= 10 || fakeDetectionRate < 15.0) {
            return 2;
        }
        return 3;
    }
}
