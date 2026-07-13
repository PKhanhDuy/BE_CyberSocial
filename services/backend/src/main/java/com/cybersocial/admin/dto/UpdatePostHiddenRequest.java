package com.cybersocial.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePostHiddenRequest(
        @NotNull Boolean hidden,
        @Size(max = 500, message = "Nội dung lý do không được vượt quá 500 ký tự") String reason
) {
}
