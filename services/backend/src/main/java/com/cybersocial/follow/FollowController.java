package com.cybersocial.follow;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.common.util.SecurityUtils;
import com.cybersocial.follow.dto.FollowCountResponse;
import com.cybersocial.follow.dto.FollowStatusResponse;
import com.cybersocial.follow.dto.FollowUserResponse;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<FollowUserResponse>> follow(@PathVariable UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Followed successfully",
                        followService.follow(SecurityUtils.requireCurrentUserId(), userId)
                ));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> cancelFollow(@PathVariable UUID userId) {
        followService.cancelFollow(SecurityUtils.requireCurrentUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Unfollowed successfully", null));
    }

    @GetMapping("/{userId}/followers/count")
    public ResponseEntity<ApiResponse<FollowCountResponse>> countFollowers(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(followService.countFollowers(userId)));
    }

    @GetMapping("/{userId}/following/count")
    public ResponseEntity<ApiResponse<FollowCountResponse>> countFollowing(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(followService.countFollowing(userId)));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<PagedResponse<FollowUserResponse>>> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50)
        );
        return ResponseEntity.ok(ApiResponse.success(followService.getFollowers(userId, pageable)));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<PagedResponse<FollowUserResponse>>> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50)
        );
        return ResponseEntity.ok(ApiResponse.success(followService.getFollowing(userId, pageable)));
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> isFollowing(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                followService.isFollowing(SecurityUtils.requireCurrentUserId(), userId)
        ));
    }
}
