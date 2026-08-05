package com.cybersocial.admin;

import com.cybersocial.admin.audit.AdminActionType;
import com.cybersocial.admin.audit.AdminAuditService;
import com.cybersocial.admin.audit.AdminTargetType;
import com.cybersocial.admin.dto.AdminUserResponse;
import com.cybersocial.admin.dto.CreateAdminUserRequest;
import com.cybersocial.admin.dto.UpdateUserStatusRequest;
import com.cybersocial.auth.repository.RefreshTokenRepository;
import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ConflictException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.user.User;
import com.cybersocial.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AdminAuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            AdminAuditService auditService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> listUsers(String query, Boolean enabled, UserRole role, Pageable pageable) {
        String normalizedQuery = normalizeQuery(query);
        Page<AdminUserResponse> page = userRepository.findForAdmin(normalizedQuery, enabled, role, pageable)
                .map(AdminUserResponse::from);
        return toPagedResponse(page);
    }

    @Override
    @Transactional
    public AdminUserResponse createAdminUser(UUID adminUserId, CreateAdminUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email này đã được sử dụng cho một tài khoản khác.");
        }

        User admin = userRepository.save(User.builder()
                .email(normalizedEmail)
                .displayName(request.displayName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());

        auditService.record(adminUserId, AdminActionType.CREATE_ADMIN, AdminTargetType.USER,
                admin.getId(), "Tạo tài khoản quản trị viên", admin.getEmail());

        return AdminUserResponse.from(admin);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserStatus(UUID adminUserId, UUID targetUserId, UpdateUserStatusRequest request) {
        boolean lock = !request.enabled();

        if (adminUserId.equals(targetUserId) && lock) {
            throw new BadRequestException(
                    "Thao tác không hợp lệ. Bạn không thể tự khóa tài khoản quản trị đang sử dụng.");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (lock) {
            if (request.reason() == null || request.reason().isBlank()) {
                throw new BadRequestException(
                        "Vui lòng nhập hoặc chọn lý do khóa tài khoản của người dùng.");
            }
            user.setEnabled(false);
            user.setLockReason(request.reason().trim());
            // Tự đăng xuất người dùng đang trực tuyến: thu hồi mọi refresh token còn hiệu lực.
            refreshTokenRepository.revokeActiveTokensForUser(user.getId(), Instant.now());
            auditService.record(adminUserId, AdminActionType.LOCK_USER, AdminTargetType.USER,
                    user.getId(), request.reason(), request.note());
        } else if (!user.isEnabled()) {
            user.setEnabled(true);
            user.setLockReason(null);
            auditService.record(adminUserId, AdminActionType.UNLOCK_USER, AdminTargetType.USER,
                    user.getId(), request.reason(), request.note());
        }

        return AdminUserResponse.from(user);
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return query.trim();
    }

    private static <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
