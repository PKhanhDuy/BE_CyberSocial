package com.cybersocial.follow;

import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.follow.dto.FollowCountResponse;
import com.cybersocial.follow.dto.FollowStatusResponse;
import com.cybersocial.follow.dto.FollowUserResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface FollowService {

    FollowUserResponse follow(UUID currentUserId, UUID targetUserId);

    void cancelFollow(UUID currentUserId, UUID targetUserId);

    FollowCountResponse countFollowers(UUID userId);

    FollowCountResponse countFollowing(UUID userId);

    PagedResponse<FollowUserResponse> getFollowers(UUID userId, Pageable pageable);

    PagedResponse<FollowUserResponse> getFollowing(UUID userId, Pageable pageable);

    FollowStatusResponse isFollowing(UUID currentUserId, UUID targetUserId);
}
