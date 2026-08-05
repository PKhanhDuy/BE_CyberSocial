package com.cybersocial.admin;

import com.cybersocial.admin.dto.AdminUserResponse;
import com.cybersocial.admin.dto.CreateAdminUserRequest;
import com.cybersocial.admin.dto.UpdateUserStatusRequest;
import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.common.util.SecurityUtils;
import com.cybersocial.user.UserRole;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) UserRole role
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                "Admin users loaded",
                adminUserService.listUsers(query, enabled, role, pageable)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserResponse>> createAdminUser(
            @Valid @RequestBody CreateAdminUserRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Đã tạo tài khoản quản trị viên",
                adminUserService.createAdminUser(SecurityUtils.requireCurrentUserId(), request)
        ));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                request.enabled() ? "User activated" : "User locked",
                adminUserService.updateUserStatus(SecurityUtils.requireCurrentUserId(), id, request)
        ));
    }
}
