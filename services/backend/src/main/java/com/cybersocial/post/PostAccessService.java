package com.cybersocial.post;

import com.cybersocial.common.exception.ForbiddenOperationException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.util.SecurityUtils;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PostAccessService {

    public void assertCanView(UUID currentUserId, Post post) {
        if (post.isHidden() && !SecurityUtils.isAdmin()) {
            throw new ResourceNotFoundException("Post not found");
        }
        if (post.getVisibility() == PostVisibility.PRIVATE
                && !post.getAuthor().getId().equals(currentUserId)
                && !SecurityUtils.isAdmin()) {
            throw new ResourceNotFoundException("Post not found");
        }
    }

    public void assertCanShare(Post rootPost) {
        if (rootPost.getVisibility() == PostVisibility.PRIVATE) {
            throw new ForbiddenOperationException("Private posts cannot be shared");
        }
    }
}
