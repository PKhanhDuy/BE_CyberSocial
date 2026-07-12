package com.cybersocial.admin;

import com.cybersocial.admin.dto.AdminUserResponse;
import com.cybersocial.admin.dto.UpdateUserStatusRequest;
import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.user.User;
import com.cybersocial.user.UserRole;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    public AdminUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
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
    public AdminUserResponse updateUserStatus(UUID adminUserId, UUID targetUserId, UpdateUserStatusRequest request) {
        if (adminUserId.equals(targetUserId) && !request.enabled()) {
            throw new BadRequestException("You cannot disable your own account");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEnabled(request.enabled());
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
