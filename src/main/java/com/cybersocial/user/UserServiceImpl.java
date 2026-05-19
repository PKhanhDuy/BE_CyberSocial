package com.cybersocial.user;

import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.user.dto.UpdateUserRequest;
import com.cybersocial.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID currentUserId) {
        return UserResponse.from(getUser(currentUserId));
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(UUID currentUserId, UpdateUserRequest request) {
        User user = getUser(currentUserId);
        user.setDisplayName(request.displayName().trim());
        return UserResponse.from(user);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
