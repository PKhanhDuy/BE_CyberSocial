package com.cybersocial.post.dto;

import com.cybersocial.post.PostShare;
import java.time.Instant;
import java.util.UUID;

public record PostShareResponse(
        UUID id,
        UUID postId,
        UUID userId,
        String authorDisplayName,
        String authorAvatarUrl,
        String content,
        Instant createdAt
) {
    public static PostShareResponse from(PostShare share) {
        return new PostShareResponse(
                share.getId(),
                share.getPost().getId(),
                share.getUser().getId(),
                share.getUser().getDisplayName(),
                share.getUser().getAvatarUrl(),
                share.getContent(),
                share.getCreatedAt()
        );
    }
}
