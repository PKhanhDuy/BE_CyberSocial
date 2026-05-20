package com.cybersocial.post.dto;

import com.cybersocial.post.Post;
import com.cybersocial.post.PostVisibility;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID authorId,
        String authorDisplayName,
        String authorAvatarUrl,
        String content,
        PostVisibility visibility,
        List<String> mediaUrls,
        Instant createdAt,
        Instant updatedAt
) {
    public static PostResponse from(Post post) {
        List<String> mediaUrls = post.getMediaUrls() == null ? List.of() : List.copyOf(post.getMediaUrls());
        return new PostResponse(
                post.getId(),
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getAuthor().getAvatarUrl(),
                post.getContent(),
                post.getVisibility(),
                mediaUrls,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
