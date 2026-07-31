package com.cybersocial.post;

import com.cybersocial.ai.PostAnalysisTriggerService;
import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.util.SecurityUtils;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ForbiddenOperationException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.post.dto.PostCommentRequest;
import com.cybersocial.post.dto.PostCommentResponse;
import com.cybersocial.post.dto.PostRequest;
import com.cybersocial.post.dto.PostResponse;
import com.cybersocial.post.dto.PostShareRequest;
import com.cybersocial.post.dto.PostVerificationResponse;
import com.cybersocial.post.dto.VerifiedNewsStatsResponse;
import com.cybersocial.user.User;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final PostStatisticsService postStatisticsService;
    private final PostAnalysisTriggerService postAnalysisTriggerService;
    private final PostVerificationService postVerificationService;

    public PostServiceImpl(
            PostRepository postRepository,
            PostLikeRepository postLikeRepository,
            PostCommentRepository postCommentRepository,
            PostShareRepository postShareRepository,
            UserRepository userRepository,
            PostStatisticsService postStatisticsService,
            PostAnalysisTriggerService postAnalysisTriggerService,
            PostVerificationService postVerificationService
    ) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.postCommentRepository = postCommentRepository;
        this.postShareRepository = postShareRepository;
        this.userRepository = userRepository;
        this.postStatisticsService = postStatisticsService;
        this.postAnalysisTriggerService = postAnalysisTriggerService;
        this.postVerificationService = postVerificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> findPosts(UUID currentUserId, Pageable pageable, UUID authorId) {
        Page<Post> posts = authorId == null
                ? postRepository.findVisiblePosts(pageable)
                : postRepository.findVisiblePostsByAuthor(authorId, pageable);
        List<UUID> postIds = posts.getContent().stream().map(Post::getId).toList();
        Map<UUID, PostStatistics> statistics = postStatisticsService.findAll(postIds);
        Set<UUID> likedPostIds = postIds.isEmpty()
                ? Set.of()
                : postLikeRepository.findLikedPostIds(currentUserId, postIds);
        Page<PostResponse> page = posts.map(post -> toResponse(
                post,
                statistics.getOrDefault(post.getId(), new PostStatistics(0, 0, 0)),
                likedPostIds.contains(post.getId())
        ));
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
    public PagedResponse<PostResponse> findVerifiedPosts(UUID currentUserId, Pageable pageable) {
        Page<Post> posts = postRepository.findVerifiedRealPosts(pageable);
        List<UUID> postIds = posts.getContent().stream().map(Post::getId).toList();
        Map<UUID, PostStatistics> statistics = postStatisticsService.findAll(postIds);
        Set<UUID> likedPostIds = postIds.isEmpty()
                ? Set.of()
                : postLikeRepository.findLikedPostIds(currentUserId, postIds);
        Page<PostResponse> page = posts.map(post -> toResponse(
                post,
                statistics.getOrDefault(post.getId(), new PostStatistics(0, 0, 0)),
                likedPostIds.contains(post.getId())
        ));
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
    public VerifiedNewsStatsResponse findVerifiedNewsStats() {
        return postVerificationService.findVerifiedNewsStats();
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse findPost(UUID currentUserId, UUID id) {
        Post post = getPost(id);
        return toResponse(
                post,
                postStatisticsService.find(id),
                postLikeRepository.existsByPostIdAndUserId(id, currentUserId)
        );
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

        Post savedPost = postRepository.save(post);
        postVerificationService.initializeForPost(savedPost);
        return toResponse(savedPost, currentUserId);
    }

    @Override
    @Transactional
    public void deletePost(UUID currentUserId, UUID postId) {
        Post post = getPost(postId);
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You can only delete your own posts");
        }
        postRepository.delete(post);
        postStatisticsService.invalidate(postId);
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
        postStatisticsService.invalidate(postId);
        postAnalysisTriggerService.onInteraction(postId);
        return toResponse(post, currentUserId);
    }

    @Override
    @Transactional
    public PostResponse unlikePost(UUID currentUserId, UUID postId) {
        Post post = getPost(postId);
        postLikeRepository.findByPostIdAndUserId(postId, currentUserId)
                .ifPresent(postLikeRepository::delete);
        postStatisticsService.invalidate(postId);
        return toResponse(post, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PostCommentResponse> findComments(UUID postId, Pageable pageable) {
        getPost(postId);
        Page<PostCommentResponse> page = postCommentRepository.findByPostIdWithUserOrderByCreatedAtAsc(postId, pageable)
                .map(PostCommentResponse::from);
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
        postStatisticsService.invalidate(postId);
        postAnalysisTriggerService.onInteraction(postId);
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
        postStatisticsService.invalidate(postId);
    }

    @Override
    @Transactional
    public PostResponse sharePost(UUID currentUserId, UUID postId, PostShareRequest request) {
        Post viewedPost = postRepository.findByIdWithSharedPost(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        Post rootPost = resolveRootPost(viewedPost);
        User user = getUser(currentUserId);
        String content = normalizeOptional(request.content());
        PostShare parentShare = resolveParentShare(viewedPost, rootPost, request.viaShareId());

        Post repost = postRepository.save(Post.builder()
                .author(user)
                .sharedPost(rootPost)
                .content(content == null ? "" : content)
                .visibility(PostVisibility.PUBLIC)
                .mediaUrls(List.of())
                .build());

        PostShare share = postShareRepository.save(PostShare.builder()
                .post(rootPost)
                .user(user)
                .content(content)
                .parentShare(parentShare)
                .repostPost(repost)
                .build());

        postStatisticsService.invalidate(rootPost.getId());
        postAnalysisTriggerService.onInteraction(rootPost.getId());
        return toResponse(repost, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PostVerificationResponse findVerification(UUID postId) {
        return postVerificationService.findByPostId(postId);
    }

    private Post getPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        if (post.isHidden() && !SecurityUtils.isAdmin()) {
            throw new ResourceNotFoundException("Post not found");
        }
        return post;
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PostResponse toResponse(Post post, UUID currentUserId) {
        UUID postId = post.getId();
        return toResponse(
                post,
                postStatisticsService.find(postId),
                postLikeRepository.existsByPostIdAndUserId(postId, currentUserId)
        );
    }

    private PostResponse toResponse(Post post, PostStatistics statistics, boolean likedByCurrentUser) {
        UUID viaShareId = null;
        if (post.getSharedPost() != null) {
            viaShareId = postShareRepository.findByRepostPostId(post.getId())
                    .map(PostShare::getId)
                    .orElse(null);
        }
        return PostResponse.from(
                post,
                statistics.likeCount(),
                statistics.commentCount(),
                statistics.shareCount(),
                likedByCurrentUser,
                viaShareId
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

    private Post resolveRootPost(Post post) {
        Post current = post;
        while (current.getSharedPost() != null) {
            UUID parentId = current.getSharedPost().getId();
            current = postRepository.findByIdWithSharedPost(parentId)
                    .orElse(current.getSharedPost());
        }
        return current;
    }

    private PostShare resolveParentShare(Post viewedPost, Post rootPost, UUID viaShareId) {
        if (viaShareId != null) {
            PostShare parentShare = postShareRepository.findById(viaShareId)
                    .orElseThrow(() -> new BadRequestException("Parent share not found"));
            if (!parentShare.getPost().getId().equals(rootPost.getId())) {
                throw new BadRequestException("Parent share does not belong to this post");
            }
            return parentShare;
        }

        if (viewedPost.getSharedPost() != null && !viewedPost.getId().equals(rootPost.getId())) {
            return postShareRepository
                    .findFirstByPostIdAndUserIdOrderByCreatedAtDesc(rootPost.getId(), viewedPost.getAuthor().getId())
                    .orElse(null);
        }

        return null;
    }
}
