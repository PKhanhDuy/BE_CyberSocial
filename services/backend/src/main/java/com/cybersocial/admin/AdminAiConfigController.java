package com.cybersocial.admin;

import com.cybersocial.admin.audit.AdminActionType;
import com.cybersocial.admin.audit.AdminAuditService;
import com.cybersocial.admin.audit.AdminTargetType;
import com.cybersocial.ai.config.AiRuntimeConfigService;
import com.cybersocial.ai.config.dto.AiConfigResponse;
import com.cybersocial.ai.config.dto.UpdateAiConfigRequest;
import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.util.SecurityUtils;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai/config")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiConfigController {

    private final AiRuntimeConfigService configService;
    private final AdminAuditService auditService;

    public AdminAiConfigController(AiRuntimeConfigService configService, AdminAuditService auditService) {
        this.configService = configService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AiConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success("AI config loaded", configService.view()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AiConfigResponse>> updateConfig(
            @Valid @RequestBody UpdateAiConfigRequest request
    ) {
        UUID adminId = SecurityUtils.requireCurrentUserId();
        AiConfigResponse updated = configService.update(request, adminId);
        auditService.record(
                adminId,
                AdminActionType.UPDATE_AI_CONFIG,
                AdminTargetType.AI_CONFIG,
                null,
                null,
                "enabled=" + updated.enabled()
                        + "; thresholds=" + updated.tierThresholds()
                        + "; debounceMinutes=" + updated.debounceMinutes()
                        + "; fakeThresholdPercent=" + updated.fakeThresholdPercent()
        );
        return ResponseEntity.ok(ApiResponse.success("Đã áp dụng cấu hình", updated));
    }
}
