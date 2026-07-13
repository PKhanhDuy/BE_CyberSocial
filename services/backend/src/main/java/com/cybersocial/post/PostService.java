package com.cybersocial.post;

import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.post.dto.PostCommentRequest;
import com.cybersocial.post.dto.PostCommentResponse;
import com.cybersocial.post.dto.PostRequest;
import com.cybersocial.post.dto.PostResponse;
import com.cybersocial.post.dto.PostShareRequest;
import com.cybersocial.post.dto.PostVerificationResponse;
import com.cybersocial.post.dto.VerifiedNewsStatsResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PagedResponse<PostResponse> findPosts(UUID currentUserId, Pageable pageable, UUID authorId);

    PagedResponse<PostResponse> findVerifiedPosts(UUID currentUserId, Pageable pageable);

    VerifiedNewsStatsResponse findVerifiedNewsStats();

    PostResponse findPost(UUID currentUserId, UUID id);

    PostResponse createPost(UUID currentUserId, PostRequest request);

    void deletePost(UUID currentUserId, UUID postId);

    PostResponse likePost(UUID currentUserId, UUID postId);

    PostResponse unlikePost(UUID currentUserId, UUID postId);

    PagedResponse<PostCommentResponse> findComments(UUID postId, Pageable pageable);

    PostCommentResponse commentPost(UUID currentUserId, UUID postId, PostCommentRequest request);

    void deleteComment(UUID currentUserId, UUID postId, UUID commentId);

    PostResponse sharePost(UUID currentUserId, UUID postId, PostShareRequest request);

    PostVerificationResponse findVerification(UUID postId);
}
