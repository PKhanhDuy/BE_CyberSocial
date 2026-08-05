package com.cybersocial.admin;

import com.cybersocial.admin.dto.AdminUserResponse;
import com.cybersocial.admin.dto.CreateAdminUserRequest;
import com.cybersocial.admin.dto.UpdateUserStatusRequest;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.user.UserRole;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    PagedResponse<AdminUserResponse> listUsers(String query, Boolean enabled, UserRole role, Pageable pageable);

    AdminUserResponse createAdminUser(UUID adminUserId, CreateAdminUserRequest request);

    AdminUserResponse updateUserStatus(UUID adminUserId, UUID targetUserId, UpdateUserStatusRequest request);
}
