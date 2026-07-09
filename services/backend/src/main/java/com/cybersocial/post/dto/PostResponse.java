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
        PostResponse sharedPost,
        long likeCount,
        long commentCount,
        long shareCount,
        boolean likedByCurrentUser,
        Instant createdAt,
        Instant updatedAt
) {
    public static PostResponse from(Post post) {
        return from(post, 0, 0, 0, false);
    }

    public static PostResponse from(
            Post post,
            long likeCount,
            long commentCount,
            long shareCount,
            boolean likedByCurrentUser
    ) {
        List<String> mediaUrls = post.getMediaUrls() == null ? List.of() : List.copyOf(post.getMediaUrls());
        return new PostResponse(
                post.getId(),
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getAuthor().getAvatarUrl(),
                post.getContent(),
                post.getVisibility(),
                mediaUrls,
                post.getSharedPost() == null ? null : PostResponse.from(post.getSharedPost()),
                likeCount,
                commentCount,
                shareCount,
                likedByCurrentUser,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
