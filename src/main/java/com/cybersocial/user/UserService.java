package com.cybersocial.user;

import com.cybersocial.user.dto.UpdateUserRequest;
import com.cybersocial.user.dto.UserResponse;
import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(UUID currentUserId);

    UserResponse updateCurrentUser(UUID currentUserId, UpdateUserRequest request);
}
