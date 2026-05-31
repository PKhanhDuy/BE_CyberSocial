package com.cybersocial.post.dto;

import com.cybersocial.post.PostComment;
import java.time.Instant;
import java.util.UUID;

public record PostCommentResponse(
        UUID id,
        UUID postId,
        UUID userId,
        String authorDisplayName,
        String authorAvatarUrl,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public static PostCommentResponse from(PostComment comment) {
        return new PostCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getId(),
                comment.getUser().getDisplayName(),
                comment.getUser().getAvatarUrl(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
