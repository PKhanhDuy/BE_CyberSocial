package com.cybersocial.post;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class PostStatisticsService {

    private static final String CACHE_NAME = "postStatistics";
    private static final long CACHE_RETRY_DELAY_NANOS = TimeUnit.SECONDS.toNanos(30);

    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostShareRepository postShareRepository;
    private final CacheManager cacheManager;
    private volatile long cacheUnavailableUntilNanos;

    public PostStatisticsService(
            PostLikeRepository postLikeRepository,
            PostCommentRepository postCommentRepository,
            PostShareRepository postShareRepository,
            CacheManager cacheManager
    ) {
        this.postLikeRepository = postLikeRepository;
        this.postCommentRepository = postCommentRepository;
        this.postShareRepository = postShareRepository;
        this.cacheManager = cacheManager;
    }

    public PostStatistics find(UUID postId) {
        return findAll(List.of(postId)).getOrDefault(postId, emptyStatistics());
    }

    public Map<UUID, PostStatistics> findAll(Collection<UUID> postIds) {
        Set<UUID> uniquePostIds = new LinkedHashSet<>(postIds);
        Map<UUID, PostStatistics> result = new LinkedHashMap<>();
        Set<UUID> missingPostIds = new LinkedHashSet<>();
        Cache cache = getCache();

        for (UUID postId : uniquePostIds) {
            PostStatistics cached = getCached(cache, postId);
            if (cached == null) {
                missingPostIds.add(postId);
            } else {
                result.put(postId, cached);
            }
        }

        if (!missingPostIds.isEmpty()) {
            Map<UUID, Long> likes = toCountMap(postLikeRepository.countByPostIds(missingPostIds));
            Map<UUID, Long> comments = toCountMap(postCommentRepository.countByPostIds(missingPostIds));
            Map<UUID, Long> shares = toCountMap(postShareRepository.countByPostIds(missingPostIds));

            for (UUID postId : missingPostIds) {
                PostStatistics statistics = new PostStatistics(
                        likes.getOrDefault(postId, 0L),
                        comments.getOrDefault(postId, 0L),
                        shares.getOrDefault(postId, 0L)
                );
                result.put(postId, statistics);
                putCached(cache, postId, statistics);
            }
        }

        return result;
    }

    public void invalidate(UUID postId) {
        Cache cache = getCache();
        if (cache == null) {
            return;
        }
        try {
            cache.evict(postId);
        } catch (RuntimeException ignored) {
            markCacheUnavailable();
        }
    }

    private Map<UUID, Long> toCountMap(List<PostCountProjection> projections) {
        Map<UUID, Long> counts = new LinkedHashMap<>();
        projections.forEach(projection -> counts.put(projection.getPostId(), projection.getCount()));
        return counts;
    }

    private Cache getCache() {
        if (System.nanoTime() < cacheUnavailableUntilNanos) {
            return null;
        }
        try {
            return cacheManager.getCache(CACHE_NAME);
        } catch (RuntimeException ignored) {
            markCacheUnavailable();
            return null;
        }
    }

    private PostStatistics getCached(Cache cache, UUID postId) {
        if (cache == null || System.nanoTime() < cacheUnavailableUntilNanos) {
            return null;
        }
        try {
            return cache.get(postId, PostStatistics.class);
        } catch (RuntimeException ignored) {
            markCacheUnavailable();
            return null;
        }
    }

    private void putCached(Cache cache, UUID postId, PostStatistics statistics) {
        if (cache == null || System.nanoTime() < cacheUnavailableUntilNanos) {
            return;
        }
        try {
            cache.put(postId, statistics);
        } catch (RuntimeException ignored) {
            markCacheUnavailable();
        }
    }

    private PostStatistics emptyStatistics() {
        return new PostStatistics(0, 0, 0);
    }

    private void markCacheUnavailable() {
        cacheUnavailableUntilNanos = System.nanoTime() + CACHE_RETRY_DELAY_NANOS;
    }
}
