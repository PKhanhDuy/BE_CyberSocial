package com.cybersocial.post.dto;

import com.cybersocial.post.PostVisibility;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostRequest(
        @Size(max = 5000, message = "Content must be 5000 characters or fewer")
        String content,

        PostVisibility visibility,

        @Size(max = 4, message = "A post can have at most 4 media items")
        List<@Size(max = 2048, message = "Media URL must be 2048 characters or fewer") String> mediaUrls
) {
}
