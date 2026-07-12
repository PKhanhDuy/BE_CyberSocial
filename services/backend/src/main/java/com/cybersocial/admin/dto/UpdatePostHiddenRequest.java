package com.cybersocial.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePostHiddenRequest(
        @NotNull Boolean hidden
) {
}
