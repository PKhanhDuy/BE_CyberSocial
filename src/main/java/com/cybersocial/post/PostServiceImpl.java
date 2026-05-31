package com.cybersocial.post;

import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ForbiddenOperationException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.post.dto.PostCommentRequest;
import com.cybersocial.post.dto.PostCommentResponse;
import com.cybersocial.post.dto.PostRequest;
import com.cybersocial.post.dto.PostResponse;
import com.cybersocial.post.dto.PostShareRequest;
import com.cybersocial.post.dto.PostShareResponse;
import com.cybersocial.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostShareRepository postShareRepository;
    private final UserRepository userRepository;

    public PostServiceImpl(
            PostRepository postRepository,
            PostLikeRepository postLikeRepository,
            PostCommentRepository postCommentRepository,
            PostShareRepository postShareRepository,
            UserRepository userRepository
    ) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.postCommentRepository = postCommentRepository;
        this.postShareRepository = postShareRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> findPosts(UUID currentUserId, Pageable pageable, UUID authorId) {
        Page<PostResponse> page = (authorId == null
                ? postRepository.findAllByOrderByCreatedAtDesc(pageable)
                : postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable))
                .map(post -> toResponse(post, currentUserId));
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse findPost(UUID currentUserId, UUID id) {
        return toResponse(getPost(id), currentUserId);
    }

    @Override
    @Transactional
    public PostResponse createPost(UUID currentUserId, PostRequest request) {
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String content = request.content() == null ? "" : request.content().trim();
        List<String> mediaUrls = normalizeMediaUrls(request.mediaUrls());

        if (content.isBlank() && mediaUrls.isEmpty()) {
            throw new BadRequestException("Post content or media is required");
        }

        Post post = Post.builder()
                .author(author)
                .content(content)
                .visibility(request.visibility() == null ? PostVisibility.PUBLIC : request.visibility())
                .mediaUrls(mediaUrls)
                .build();
        // Post post = new Post();
        // post.setAuthor(author);

        return toResponse(postRepository.save(post), currentUserId);
    }

    @Override
    @Transactional
    public void deletePost(UUID currentUserId, UUID postId) {
        Post post = getPost(postId);
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You can only delete your own posts");
        }
        postRepository.delete(post);
    }

    @Override
    @Transactional
    public PostResponse likePost(UUID currentUserId, UUID postId) {
        Post post = getPost(postId);
        User user = getUser(currentUserId);
        if (postLikeRepository.findByPostIdAndUserId(postId, currentUserId).isEmpty()) {
            postLikeRepository.save(PostLike.builder()
                    .post(post)
                    .user(user)
                    .build());
        }
        return toResponse(post, currentUserId);
    }

    @Override
    @Transactional
    public PostResponse unlikePost(UUID currentUserId, UUID postId) {
        Post post = getPost(postId);
        postLikeRepository.findByPostIdAndUserId(postId, currentUserId)
                .ifPresent(postLikeRepository::delete);
        return toResponse(post, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostCommentResponse> findComments(UUID postId) {
        getPost(postId);
        return postCommentRepository.findByPostIdWithUserOrderByCreatedAtAsc(postId).stream()
                .map(PostCommentResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PostCommentResponse commentPost(UUID currentUserId, UUID postId, PostCommentRequest request) {
        Post post = getPost(postId);
        User user = getUser(currentUserId);
        String content = normalizeRequired(request.content(), "Comment content is required");

        PostComment comment = postCommentRepository.save(PostComment.builder()
                .post(post)
                .user(user)
                .content(content)
                .build());
        return PostCommentResponse.from(comment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID currentUserId, UUID postId, UUID commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (!comment.getPost().getId().equals(postId)) {
            throw new ResourceNotFoundException("Comment not found");
        }
        boolean isCommentOwner = comment.getUser().getId().equals(currentUserId);
        boolean isPostOwner = comment.getPost().getAuthor().getId().equals(currentUserId);
        if (!isCommentOwner && !isPostOwner) {
            throw new ForbiddenOperationException("You can only delete your own comments");
        }
        postCommentRepository.delete(comment);
    }

    @Override
    @Transactional
    public PostShareResponse sharePost(UUID currentUserId, UUID postId, PostShareRequest request) {
        Post post = getPost(postId);
        User user = getUser(currentUserId);
        String content = normalizeOptional(request.content());

        PostShare share = postShareRepository.save(PostShare.builder()
                .post(post)
                .user(user)
                .content(content)
                .build());
        return PostShareResponse.from(share);
    }

    private Post getPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PostResponse toResponse(Post post, UUID currentUserId) {
        UUID postId = post.getId();
        return PostResponse.from(
                post,
                postLikeRepository.countByPostId(postId),
                postCommentRepository.countByPostId(postId),
                postShareRepository.countByPostId(postId),
                postLikeRepository.existsByPostIdAndUserId(postId, currentUserId)
        );
    }

    private List<String> normalizeMediaUrls(List<String> mediaUrls) {
        if (mediaUrls == null) {
            return List.of();
        }
        return mediaUrls.stream()
                .map(url -> url == null ? "" : url.trim())
                .filter(url -> !url.isBlank())
                .toList();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
