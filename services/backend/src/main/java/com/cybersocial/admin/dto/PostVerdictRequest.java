package com.cybersocial.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostVerdictRequest(
        @NotNull(message = "Vui lòng chọn hành động xử lý (xác nhận hoặc bác bỏ nhãn).")
        AdminVerdictDecision decision,
        @NotBlank(message = "Vui lòng nhập lý do hoặc ghi chú xử lý tin giả để lưu vào nhật ký hệ thống.")
        @Size(max = 500, message = "Nội dung ghi chú không được vượt quá 500 ký tự")
        String note
) {
}
