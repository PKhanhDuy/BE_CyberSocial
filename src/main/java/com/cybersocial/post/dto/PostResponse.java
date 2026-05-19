package com.cybersocial.post.dto;

import com.cybersocial.post.Post;
import com.cybersocial.post.PostVisibility;
import java.time.Instant;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID authorId,
        String authorDisplayName,
        String content,
        PostVisibility visibility,
        Instant createdAt,
        Instant updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getContent(),
                post.getVisibility(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
