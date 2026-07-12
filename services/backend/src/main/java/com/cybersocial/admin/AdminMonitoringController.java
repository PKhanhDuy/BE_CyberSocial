package com.cybersocial.admin;

import com.cybersocial.ai.AIMonitoringService;
import com.cybersocial.ai.dto.AIMonitoringStatsResponse;
import com.cybersocial.ai.dto.PendingScanPostResponse;
import com.cybersocial.common.response.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai/monitoring")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMonitoringController {

    private final AIMonitoringService monitoringService;

    public AdminMonitoringController(AIMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AIMonitoringStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("AI monitoring stats loaded", monitoringService.getStats()));
    }

    @GetMapping("/pending-scan")
    public ResponseEntity<ApiResponse<List<PendingScanPostResponse>>> getPendingScanPosts(
            @RequestParam(defaultValue = "3") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Pending scan queue loaded",
                monitoringService.getPendingScanPosts(limit)
        ));
    }
}
