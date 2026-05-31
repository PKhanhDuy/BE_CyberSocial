package com.cybersocial.post;

import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.post.dto.PostRequest;
import com.cybersocial.post.dto.PostResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PagedResponse<PostResponse> findPosts(Pageable pageable, UUID authorId);

    PostResponse findPost(UUID id);

    PostResponse createPost(UUID currentUserId, PostRequest request);

    void deletePost(UUID currentUserId, UUID postId);
}
