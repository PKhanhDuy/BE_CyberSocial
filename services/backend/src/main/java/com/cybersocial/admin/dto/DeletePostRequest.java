package com.cybersocial.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeletePostRequest(
        @NotBlank(message = "Vui lòng chọn tiêu chuẩn cộng đồng bị vi phạm để tiếp tục gỡ bài viết.")
        @Size(max = 500, message = "Nội dung lý do không được vượt quá 500 ký tự")
        String reason
) {
}
