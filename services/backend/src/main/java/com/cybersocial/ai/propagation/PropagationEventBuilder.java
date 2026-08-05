package com.cybersocial.ai.propagation;

import com.cybersocial.ai.dto.InteractionEventDto;
import com.cybersocial.ai.dto.TreeFeaturesDto;
import com.cybersocial.ai.dto.UserProfileFeaturesDto;
import com.cybersocial.post.Post;
import com.cybersocial.post.PostComment;
import com.cybersocial.post.PostCommentRepository;
import com.cybersocial.post.PostLike;
import com.cybersocial.post.PostLikeRepository;
import com.cybersocial.post.PostShare;
import com.cybersocial.post.PostShareRepository;
import com.cybersocial.user.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropagationEventBuilder {

    private static final int MAX_EVENTS = 256;

    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostShareRepository postShareRepository;
    private final UserSocialStatsService userSocialStatsService;

    public PropagationEventBuilder(
            PostLikeRepository postLikeRepository,
            PostCommentRepository postCommentRepository,
            PostShareRepository postShareRepository,
            UserSocialStatsService userSocialStatsService
    ) {
        this.postLikeRepository = postLikeRepository;
        this.postCommentRepository = postCommentRepository;
        this.postShareRepository = postShareRepository;
        this.userSocialStatsService = userSocialStatsService;
    }

    @Transactional(readOnly = true)
    public List<InteractionEventDto> build(Post post) {
        UUID rootEventId = post.getId();
        List<PostLike> likes = postLikeRepository.findByPostIdWithUserOrderByCreatedAtAsc(post.getId());
        List<PostComment> comments = postCommentRepository
                .findByPostIdWithUserOrderByCreatedAtAsc(post.getId(), Pageable.unpaged())
                .getContent();
        List<PostShare> shares = postShareRepository.findByPostIdWithUserOrderByCreatedAtAsc(post.getId());

        Map<UUID, Integer> shareDepthById = computeShareDepths(shares, rootEventId);
        Map<UUID, Integer> shareChildCount = computeShareChildCounts(shares);
        int rootShareOutDegree = (int) shares.stream()
                .filter(share -> share.getParentShare() == null)
                .count();

        Set<UUID> actorIds = new HashSet<>();
        actorIds.add(post.getAuthor().getId());
        likes.forEach(like -> actorIds.add(like.getUser().getId()));
        comments.forEach(comment -> actorIds.add(comment.getUser().getId()));
        shares.forEach(share -> actorIds.add(share.getUser().getId()));

        Map<UUID, UserSocialStats> statsByUser = userSocialStatsService.loadStats(actorIds);
        long articleId = stableArticleId(post.getId());

        List<TimelineEntry> timeline = new ArrayList<>();
        timeline.add(new TimelineEntry(
                post.getCreatedAt(),
                0,
                buildEvent(
                        rootEventId.toString(),
                        null,
                        post.getAuthor(),
                        articleId,
                        post.getCreatedAt(),
                        0,
                        "tweet",
                        0,
                        post.getContent() == null ? 0 : post.getContent().length(),
                        statsByUser,
                        0.0,
                        rootShareOutDegree
                )
        ));

        for (PostLike like : likes) {
            timeline.add(new TimelineEntry(
                    like.getCreatedAt(),
                    1,
                    buildEvent(
                            like.getId().toString(),
                            rootEventId.toString(),
                            like.getUser(),
                            articleId,
                            like.getCreatedAt(),
                            null,
                            "like",
                            0,
                            0,
                            statsByUser,
                            0.0,
                            0
                    )
            ));
        }

        for (PostComment comment : comments) {
            timeline.add(new TimelineEntry(
                    comment.getCreatedAt(),
                    2,
                    buildEvent(
                            comment.getId().toString(),
                            rootEventId.toString(),
                            comment.getUser(),
                            articleId,
                            comment.getCreatedAt(),
                            null,
                            "comment",
                            0,
                            comment.getContent() == null ? 0 : comment.getContent().length(),
                            statsByUser,
                            0.0,
                            0
                    )
            ));
        }

        int shareCountSoFar = 0;
        for (PostShare share : shares) {
            shareCountSoFar++;
            boolean hasQuote = share.getContent() != null && !share.getContent().isBlank();
            int textLen = hasQuote ? share.getContent().length() : 0;
            String eventType = hasQuote ? "share" : "retweet";
            UUID shareId = share.getId();
            double depth = shareDepthById.getOrDefault(shareId, 1);
            String parentEventId = share.getParentShare() == null
                    ? rootEventId.toString()
                    : share.getParentShare().getId().toString();
            timeline.add(new TimelineEntry(
                    share.getCreatedAt(),
                    3,
                    buildEvent(
                            shareId.toString(),
                            parentEventId,
                            share.getUser(),
                            articleId,
                            share.getCreatedAt(),
                            null,
                            eventType,
                            shareCountSoFar,
                            textLen,
                            statsByUser,
                            depth,
                            shareChildCount.getOrDefault(shareId, 0)
                    )
            ));
        }

        timeline.sort(Comparator
                .comparing(TimelineEntry::timestamp)
                .thenComparingInt(TimelineEntry::kindOrder));

        List<InteractionEventDto> events = new ArrayList<>();
        for (int index = 0; index < timeline.size() && index < MAX_EVENTS; index++) {
            TimelineEntry entry = timeline.get(index);
            InteractionEventDto base = entry.event();
            events.add(new InteractionEventDto(
                    base.eventId(),
                    base.parentEventId(),
                    base.srcUserId(),
                    base.dstArticleId(),
                    base.timestampUnix(),
                    (double) index,
                    base.eventType(),
                    base.retweetCount(),
                    base.favoriteCount(),
                    base.textLen(),
                    base.actorDisplayName(),
                    base.userProfile(),
                    base.tree()
            ));
        }

        return events;
    }

    public static long stableArticleId(UUID postId) {
        long value = postId.getMostSignificantBits() ^ postId.getLeastSignificantBits();
        return value & 0x7FFFFFFFFFFFFFFFL;
    }

    public static long stableUserId(UUID userId) {
        long value = userId.getMostSignificantBits() ^ userId.getLeastSignificantBits();
        return value & 0x7FFFFFFFFFFFFFFFL;
    }

    private Map<UUID, Integer> computeShareDepths(List<PostShare> shares, UUID rootEventId) {
        Map<UUID, PostShare> shareById = new HashMap<>();
        shares.forEach(share -> shareById.put(share.getId(), share));

        Map<UUID, Integer> depthById = new HashMap<>();
        for (PostShare share : shares) {
            resolveShareDepth(share, shareById, depthById, rootEventId);
        }
        return depthById;
    }

    private int resolveShareDepth(
            PostShare share,
            Map<UUID, PostShare> shareById,
            Map<UUID, Integer> depthById,
            UUID rootEventId
    ) {
        if (depthById.containsKey(share.getId())) {
            return depthById.get(share.getId());
        }
        if (share.getParentShare() == null) {
            depthById.put(share.getId(), 1);
            return 1;
        }

        UUID parentId = share.getParentShare().getId();
        PostShare parentShare = shareById.get(parentId);
        int depth;
        if (parentShare == null) {
            depth = 1;
        } else {
            depth = resolveShareDepth(parentShare, shareById, depthById, rootEventId) + 1;
        }
        depthById.put(share.getId(), depth);
        return depth;
    }

    private Map<UUID, Integer> computeShareChildCounts(List<PostShare> shares) {
        Map<UUID, Integer> childCount = new HashMap<>();
        for (PostShare share : shares) {
            if (share.getParentShare() == null) {
                continue;
            }
            UUID parentId = share.getParentShare().getId();
            childCount.merge(parentId, 1, Integer::sum);
        }
        return childCount;
    }

    private InteractionEventDto buildEvent(
            String eventId,
            String parentEventId,
            User user,
            long articleId,
            Instant timestamp,
            Integer eventOrder,
            String eventType,
            double retweetCount,
            int textLen,
            Map<UUID, UserSocialStats> statsByUser,
            double depth,
            int outDegree
    ) {
        UserSocialStats stats = statsByUser.get(user.getId());
        UserProfileFeaturesDto profile = stats == null ? null : new UserProfileFeaturesDto(
                stats.logFollowers(),
                stats.logFollowing(),
                stats.logStatuses(),
                stats.accountCreatedUnix(),
                stats.hasProfile()
        );

        return new InteractionEventDto(
                eventId,
                parentEventId,
                stableUserId(user.getId()),
                articleId,
                (double) timestamp.getEpochSecond(),
                eventOrder == null ? null : eventOrder.doubleValue(),
                eventType,
                retweetCount,
                0.0,
                (double) textLen,
                user.getDisplayName(),
                profile,
                new TreeFeaturesDto(depth, (double) outDegree)
        );
    }

    private record TimelineEntry(Instant timestamp, int kindOrder, InteractionEventDto event) {
    }
}
