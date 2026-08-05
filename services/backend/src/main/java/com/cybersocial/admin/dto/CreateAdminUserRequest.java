package com.cybersocial.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
        @Email(message = "Email không hợp lệ")
        @NotBlank(message = "Vui lòng nhập email")
        String email,

        @NotBlank(message = "Vui lòng nhập tên hiển thị")
        @Size(min = 2, max = 120, message = "Tên hiển thị phải từ 2 đến 120 ký tự")
        String displayName,

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        String password
) {
}
