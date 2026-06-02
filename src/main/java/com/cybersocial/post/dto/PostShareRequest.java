package com.cybersocial.post.dto;

import jakarta.validation.constraints.Size;

public record PostShareRequest(
        @Size(max = 1000, message = "Share content must be 1000 characters or fewer")
        String content
) {
}
