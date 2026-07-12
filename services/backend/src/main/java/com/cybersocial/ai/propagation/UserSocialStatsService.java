package com.cybersocial.ai.propagation;

import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.follow.FollowRepository;
import com.cybersocial.post.PostRepository;
import com.cybersocial.user.User;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSocialStatsService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;

    public UserSocialStatsService(
            UserRepository userRepository,
            FollowRepository followRepository,
            PostRepository postRepository
    ) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, UserSocialStats> loadStats(Collection<UUID> userIds) {
        Map<UUID, UserSocialStats> statsByUser = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return statsByUser;
        }

        for (UUID userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            long followers = followRepository.countByFollowingId(userId);
            long following = followRepository.countByFollowerId(userId);
            long posts = postRepository.countByAuthorId(userId);

            statsByUser.put(userId, new UserSocialStats(
                    log1p(followers),
                    log1p(following),
                    log1p(posts),
                    user.getCreatedAt().getEpochSecond(),
                    hasProfile(user)
            ));
        }

        return statsByUser;
    }

    private static double log1p(long value) {
        return Math.log1p(Math.max(value, 0L));
    }

    private static double hasProfile(User user) {
        boolean hasAvatar = user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank();
        boolean hasDisplayName = user.getDisplayName() != null && !user.getDisplayName().isBlank();
        return hasAvatar || hasDisplayName ? 1.0 : 0.0;
    }
}
