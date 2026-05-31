package com.cybersocial.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCommentRequest(
        @NotBlank(message = "Comment content is required")
        @Size(max = 1000, message = "Comment must be 1000 characters or fewer")
        String content
) {
}
