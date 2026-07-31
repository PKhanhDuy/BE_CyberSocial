package com.cybersocial.demo;

import com.cybersocial.ai.PostAnalysisTriggerService;
import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.demo.dto.DemoUserGenerationResponse;
import com.cybersocial.demo.dto.PropagationSimulationRequest;
import com.cybersocial.demo.dto.PropagationSimulationResponse;
import com.cybersocial.post.PostRepository;
import com.cybersocial.post.PostCommentRepository;
import com.cybersocial.post.PostLikeRepository;
import com.cybersocial.post.PostShareRepository;
import com.cybersocial.post.PostStatisticsService;
import com.cybersocial.user.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoPropagationServiceImpl implements DemoPropagationService {

    private static final int DEFAULT_DEMO_USER_COUNT = 50;
    private static final int DEFAULT_DURATION_SECONDS = 60;
    private static final String DEMO_EMAIL_DOMAIN = "cybersocial-demo.local";
    private static final String DEMO_PASSWORD = "Demo@123456";

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostShareRepository postShareRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final PostStatisticsService postStatisticsService;
    private final PostAnalysisTriggerService postAnalysisTriggerService;
    private final Random random = new Random();

    public DemoPropagationServiceImpl(
            UserRepository userRepository,
            PostRepository postRepository,
            PostLikeRepository postLikeRepository,
            PostCommentRepository postCommentRepository,
            PostShareRepository postShareRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager,
            PostStatisticsService postStatisticsService,
            PostAnalysisTriggerService postAnalysisTriggerService
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.postCommentRepository = postCommentRepository;
        this.postShareRepository = postShareRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
        this.postStatisticsService = postStatisticsService;
        this.postAnalysisTriggerService = postAnalysisTriggerService;
    }

    @Override
    @Transactional
    public DemoUserGenerationResponse generateDemoUsers(Integer count) {
        int requestedCount = normalize(count, DEFAULT_DEMO_USER_COUNT, 1, 300);
        DemoUsersResult result = ensureDemoUsers(requestedCount);
        return new DemoUserGenerationResponse(requestedCount, result.users().size(), result.usersCreated());
    }

    @Override
    @Transactional
    public PropagationSimulationResponse simulatePropagation(PropagationSimulationRequest request) {
        UUID postId = request.postId();
        postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        int demoUserCount = normalize(request.demoUserCount(), DEFAULT_DEMO_USER_COUNT, 1, 300);
        int requestedShares = normalize(request.shares(), 40, 0, 300);
        int requestedLikes = normalize(request.likes(), 80, 0, 300);
        int requestedComments = normalize(request.comments(), 15, 0, 300);
        int durationSeconds = normalize(request.durationSeconds(), DEFAULT_DURATION_SECONDS, 1, 3600);
        PropagationPattern pattern = request.pattern() == null ? PropagationPattern.VIRAL_BURST : request.pattern();

        DemoUsersResult demoUsersResult = ensureDemoUsers(demoUserCount);
        List<User> demoUsers = new ArrayList<>(demoUsersResult.users());
        java.util.Collections.shuffle(demoUsers, random);

        Instant endedAt = Instant.now();
        Instant startedAt = endedAt.minusSeconds(durationSeconds);

        int likesCreated = createLikes(postId, demoUsers, requestedLikes, startedAt, durationSeconds, pattern);
        int commentsCreated = createComments(postId, demoUsers, requestedComments, startedAt, durationSeconds, pattern);
        int sharesCreated = createShares(postId, demoUsers, requestedShares, startedAt, durationSeconds, pattern);
        postStatisticsService.invalidate(postId);
        postAnalysisTriggerService.afterPropagationSimulation(postId);

        return new PropagationSimulationResponse(
                postId,
                pattern,
                demoUsers.size(),
                demoUsersResult.usersCreated(),
                likesCreated,
                commentsCreated,
                sharesCreated,
                postLikeRepository.countByPostId(postId),
                postCommentRepository.countByPostId(postId),
                postShareRepository.countByPostId(postId),
                durationSeconds,
                startedAt,
                endedAt
        );
    }

    private DemoUsersResult ensureDemoUsers(int requestedCount) {
        List<User> demoUsers = new ArrayList<>(userRepository.findByDemoUserTrueOrderByEmailAsc());
        if (demoUsers.size() >= requestedCount) {
            return new DemoUsersResult(demoUsers.subList(0, requestedCount), 0);
        }

        Set<String> existingEmails = new HashSet<>();
        userRepository.findAll().forEach(user -> existingEmails.add(user.getEmail().toLowerCase()));

        List<User> usersToCreate = new ArrayList<>();
        int index = 1;
        while (demoUsers.size() + usersToCreate.size() < requestedCount) {
            String email = "demo.user.%03d@%s".formatted(index, DEMO_EMAIL_DOMAIN);
            if (!existingEmails.contains(email.toLowerCase())) {
                usersToCreate.add(User.builder()
                        .email(email)
                        .displayName("Demo User %03d".formatted(index))
                        .avatarUrl("https://i.pravatar.cc/150?u=cybersocial-demo-%03d".formatted(index))
                        .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                        .enabled(true)
                        .demoUser(true)
                        .build());
                existingEmails.add(email.toLowerCase());
            }
            index++;
        }

        demoUsers.addAll(userRepository.saveAll(usersToCreate));
        return new DemoUsersResult(demoUsers, usersToCreate.size());
    }

    private int createLikes(
            UUID postId,
            List<User> users,
            int requestedLikes,
            Instant startedAt,
            int durationSeconds,
            PropagationPattern pattern
    ) {
        int created = 0;
        int total = Math.min(requestedLikes, users.size());
        for (int i = 0; i < total; i++) {
            User user = users.get(i);
            Instant createdAt = scheduleAt(i, total, startedAt, durationSeconds, pattern);
            created += insertLike(postId, user.getId(), createdAt);
        }
        return created;
    }

    private int createComments(
            UUID postId,
            List<User> users,
            int requestedComments,
            Instant startedAt,
            int durationSeconds,
            PropagationPattern pattern
    ) {
        if (users.isEmpty()) {
            return 0;
        }

        int created = 0;
        for (int i = 0; i < requestedComments; i++) {
            User user = users.get(i % users.size());
            Instant createdAt = scheduleAt(i, requestedComments, startedAt, durationSeconds, pattern);
            String content = "Demo propagation comment #%d".formatted(i + 1);
            created += insertComment(postId, user.getId(), content, createdAt);
        }
        return created;
    }

    private int createShares(
            UUID postId,
            List<User> users,
            int requestedShares,
            Instant startedAt,
            int durationSeconds,
            PropagationPattern pattern
    ) {
        int created = 0;
        int total = Math.min(requestedShares, users.size());
        List<UUID> chainShareIds = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            User user = users.get(i);
            Instant createdAt = scheduleAt(i, total, startedAt, durationSeconds, pattern);
            UUID parentShareId = pattern == PropagationPattern.CHAIN
                    ? resolveChainParentShareId(chainShareIds)
                    : null;
            UUID shareId = insertShare(postId, user.getId(), createdAt, parentShareId);
            created++;
            if (pattern == PropagationPattern.CHAIN) {
                chainShareIds.add(shareId);
            }
        }
        return created;
    }

    /**
     * Build a branching share tree: some shares re-post from root, others from prior shares.
     * Produces a multi-branch propagation graph instead of a single straight line.
     */
    private UUID resolveChainParentShareId(List<UUID> existingShareIds) {
        if (existingShareIds.isEmpty()) {
            return null;
        }
        if (random.nextDouble() < 0.35) {
            return null;
        }
        int window = Math.min(existingShareIds.size(), 8);
        int start = existingShareIds.size() - window;
        return existingShareIds.get(start + random.nextInt(window));
    }

    private int insertLike(UUID postId, UUID userId, Instant createdAt) {
        return entityManager.createNativeQuery("""
                        insert into post_likes (id, post_id, user_id, is_synthetic, created_at)
                        values (:id, :postId, :userId, true, :createdAt)
                        on conflict (post_id, user_id) do nothing
                        """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("postId", postId)
                .setParameter("userId", userId)
                .setParameter("createdAt", createdAt)
                .executeUpdate();
    }

    private int insertComment(UUID postId, UUID userId, String content, Instant createdAt) {
        return entityManager.createNativeQuery("""
                        insert into post_comments (id, post_id, user_id, content, is_synthetic, created_at, updated_at)
                        values (:id, :postId, :userId, :content, true, :createdAt, :createdAt)
                        """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("postId", postId)
                .setParameter("userId", userId)
                .setParameter("content", content)
                .setParameter("createdAt", createdAt)
                .executeUpdate();
    }

    private UUID insertShare(UUID postId, UUID userId, Instant createdAt, UUID parentShareId) {
        UUID shareId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                        insert into post_shares (id, post_id, user_id, parent_share_id, content, is_synthetic, created_at)
                        values (:id, :postId, :userId, :parentShareId, null, true, :createdAt)
                        """)
                .setParameter("id", shareId)
                .setParameter("postId", postId)
                .setParameter("userId", userId)
                .setParameter("parentShareId", parentShareId)
                .setParameter("createdAt", createdAt)
                .executeUpdate();
        return shareId;
    }

    private Instant scheduleAt(
            int index,
            int total,
            Instant startedAt,
            int durationSeconds,
            PropagationPattern pattern
    ) {
        if (total <= 0) {
            return startedAt;
        }

        double ordinal = total == 1 ? 1.0 : (double) index / (double) (total - 1);
        double position = switch (pattern) {
            case ORGANIC -> (index + 1.0) / (total + 1.0);
            case VIRAL_BURST -> Math.pow((index + 1.0) / total, 2.35);
            case COORDINATED -> 0.55 + ((ordinal - 0.5) * 0.12);
            case CHAIN -> (index + 1.0) / (total + 1.0);
        };

        long millis = Math.round(clamp(position, 0.0, 1.0) * durationSeconds * 1000.0);
        return startedAt.plusMillis(millis);
    }

    private int normalize(Integer value, int fallback, int min, int max) {
        int normalized = value == null ? fallback : value;
        return Math.min(Math.max(normalized, min), max);
    }

    private double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    private record DemoUsersResult(List<User> users, int usersCreated) {
    }
}
