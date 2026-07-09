package com.cybersocial.user;

import com.cybersocial.user.dto.UpdateAvatarRequest;
import com.cybersocial.user.dto.UpdateCoverRequest;
import com.cybersocial.user.dto.UpdateUserRequest;
import com.cybersocial.user.dto.UserResponse;
import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(UUID currentUserId);

    UserResponse getUser(UUID userId);

    UserResponse updateCurrentUser(UUID currentUserId, UpdateUserRequest request);

    UserResponse updateCurrentUserAvatar(UUID currentUserId, UpdateAvatarRequest request);

    UserResponse updateCurrentUserCover(UUID currentUserId, UpdateCoverRequest request);
}
