package com.cybersocial.admin.dto;

import com.cybersocial.post.Post;
import com.cybersocial.post.PostVisibility;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminPostResponse(
        UUID id,
        UUID authorId,
        String authorDisplayName,
        String authorAvatarUrl,
        String content,
        PostVisibility visibility,
        List<String> mediaUrls,
        boolean hidden,
        Instant hiddenAt,
        long likeCount,
        long commentCount,
        long shareCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminPostResponse from(Post post, long likeCount, long commentCount, long shareCount) {
        List<String> mediaUrls = post.getMediaUrls() == null ? List.of() : List.copyOf(post.getMediaUrls());
        return new AdminPostResponse(
                post.getId(),
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getAuthor().getAvatarUrl(),
                post.getContent(),
                post.getVisibility(),
                mediaUrls,
                post.isHidden(),
                post.getHiddenAt(),
                likeCount,
                commentCount,
                shareCount,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
