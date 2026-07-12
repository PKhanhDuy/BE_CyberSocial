package com.cybersocial.admin;

import com.cybersocial.admin.dto.AdminStatsResponse;
import com.cybersocial.ai.AIMonitoringService;
import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.follow.FollowRepository;
import com.cybersocial.friend.FriendshipRepository;
import com.cybersocial.friend.FriendshipStatus;
import com.cybersocial.message.MessageRepository;
import com.cybersocial.post.PostRepository;
import com.cybersocial.story.StoryRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FriendshipRepository friendshipRepository;
    private final FollowRepository followRepository;
    private final MessageRepository messageRepository;
    private final StoryRepository storyRepository;
    private final AIMonitoringService aiMonitoringService;

    public AdminStatsService(
            UserRepository userRepository,
            PostRepository postRepository,
            FriendshipRepository friendshipRepository,
            FollowRepository followRepository,
            MessageRepository messageRepository,
            StoryRepository storyRepository,
            AIMonitoringService aiMonitoringService
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.friendshipRepository = friendshipRepository;
        this.followRepository = followRepository;
        this.messageRepository = messageRepository;
        this.storyRepository = storyRepository;
        this.aiMonitoringService = aiMonitoringService;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalPosts = postRepository.countBySyntheticFalse();
        long visiblePosts = postRepository.countVisiblePosts();
        long hiddenPosts = postRepository.countByHiddenTrue();

        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByEnabledTrue(),
                userRepository.countByEnabledFalse(),
                totalPosts,
                visiblePosts,
                hiddenPosts,
                friendshipRepository.countByStatus(FriendshipStatus.ACCEPTED),
                followRepository.count(),
                messageRepository.count(),
                storyRepository.countActiveStories(Instant.now()),
                aiMonitoringService.getStats()
        );
    }
}
