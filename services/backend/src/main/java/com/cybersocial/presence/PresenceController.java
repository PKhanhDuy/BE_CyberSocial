package com.cybersocial.presence;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.util.SecurityUtils;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping("/friends")
    public ResponseEntity<ApiResponse<List<UUID>>> onlineFriends() {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(presenceService.listOnlineFriendIds(currentUserId)));
    }
}
