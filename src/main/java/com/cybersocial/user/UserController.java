package com.cybersocial.user;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.util.SecurityUtils;
import com.cybersocial.user.dto.UpdateAvatarRequest;
import com.cybersocial.user.dto.UpdateCoverRequest;
import com.cybersocial.user.dto.UpdateUserRequest;
import com.cybersocial.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser(SecurityUtils.requireCurrentUserId())));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated",
                userService.updateCurrentUser(SecurityUtils.requireCurrentUserId(), request)
        ));
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Avatar updated",
                userService.updateCurrentUserAvatar(SecurityUtils.requireCurrentUserId(), request)
        ));
    }

    @PutMapping("/me/cover")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyCover(@Valid @RequestBody UpdateCoverRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cover updated",
                userService.updateCurrentUserCover(SecurityUtils.requireCurrentUserId(), request)
        ));
    }
}
