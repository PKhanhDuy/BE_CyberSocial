package com.cybersocial.friend;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.common.util.SecurityUtils;
import com.cybersocial.friend.dto.FriendUserResponse;
import com.cybersocial.friend.dto.FriendshipResponse;
import com.cybersocial.friend.dto.FriendshipStateResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> friends() {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.findFriends(SecurityUtils.requireCurrentUserId())));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> incomingRequests() {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.findIncomingRequests(SecurityUtils.requireCurrentUserId())));
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> outgoingRequests() {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.findOutgoingRequests(SecurityUtils.requireCurrentUserId())));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<FriendUserResponse>>> searchUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizedSize, Sort.by(Sort.Direction.ASC, "displayName"));
        return ResponseEntity.ok(ApiResponse.success(friendshipService.searchUsers(SecurityUtils.requireCurrentUserId(), query, pageable)));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<FriendshipStateResponse>> friendshipState(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                friendshipService.findState(SecurityUtils.requireCurrentUserId(), userId)
        ));
    }

    @PostMapping("/requests/{userId}")
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendRequest(@PathVariable UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Friend request sent", friendshipService.sendRequest(SecurityUtils.requireCurrentUserId(), userId)));
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptRequest(@PathVariable UUID requestId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Friend request accepted",
                friendshipService.acceptRequest(SecurityUtils.requireCurrentUserId(), requestId)
        ));
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<Void>> deleteRequest(@PathVariable UUID requestId) {
        friendshipService.deleteRequest(SecurityUtils.requireCurrentUserId(), requestId);
        return ResponseEntity.ok(ApiResponse.success("Friend request removed", null));
    }

    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(@PathVariable UUID friendshipId) {
        friendshipService.removeFriend(SecurityUtils.requireCurrentUserId(), friendshipId);
        return ResponseEntity.ok(ApiResponse.success("Friend removed", null));
    }
}
