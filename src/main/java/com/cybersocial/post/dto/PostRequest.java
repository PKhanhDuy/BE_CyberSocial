package com.cybersocial.post.dto;

import com.cybersocial.post.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 5000, message = "Content must be 5000 characters or fewer")
        String content,

        PostVisibility visibility
) {
}
