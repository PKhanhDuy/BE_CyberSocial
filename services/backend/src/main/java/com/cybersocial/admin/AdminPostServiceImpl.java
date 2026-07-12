package com.cybersocial.admin;

import com.cybersocial.admin.dto.AdminFakePostResponse;
import com.cybersocial.admin.dto.AdminPostResponse;
import com.cybersocial.admin.dto.UpdatePostHiddenRequest;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.post.Post;
import com.cybersocial.post.PostRepository;
import com.cybersocial.post.PostStatistics;
import com.cybersocial.post.PostStatisticsService;
import com.cybersocial.post.PostVerification;
import com.cybersocial.post.PostVerificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPostServiceImpl implements AdminPostService {

    private final PostRepository postRepository;
    private final PostVerificationRepository postVerificationRepository;
    private final PostStatisticsService postStatisticsService;

    public AdminPostServiceImpl(
            PostRepository postRepository,
            PostVerificationRepository postVerificationRepository,
            PostStatisticsService postStatisticsService
    ) {
        this.postRepository = postRepository;
        this.postVerificationRepository = postVerificationRepository;
        this.postStatisticsService = postStatisticsService;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AdminPostResponse> listPosts(String query, Boolean hidden, Pageable pageable) {
        String normalizedQuery = normalizeQuery(query);
        Page<Post> posts = postRepository.findForAdmin(normalizedQuery, hidden, pageable);
        return toAdminPostPage(posts);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AdminFakePostResponse> listFakePosts(Pageable pageable) {
        Page<Post> posts = postRepository.findFakePosts(pageable);
        List<UUID> postIds = posts.getContent().stream().map(Post::getId).toList();
        Map<UUID, PostVerification> verificationByPostId = postVerificationRepository.findByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(verification -> verification.getPost().getId(), Function.identity()));

        Page<AdminFakePostResponse> page = posts.map(post -> AdminFakePostResponse.from(
                post,
                verificationByPostId.get(post.getId())
        ));
        return toPagedResponse(page);
    }

    @Override
    @Transactional
    public AdminPostResponse updatePostHidden(UUID postId, UpdatePostHiddenRequest request) {
        Post post = getPost(postId);
        post.setHidden(request.hidden());
        post.setHiddenAt(request.hidden() ? Instant.now() : null);
        PostStatistics statistics = postStatisticsService.find(postId);
        return AdminPostResponse.from(
                post,
                statistics.likeCount(),
                statistics.commentCount(),
                statistics.shareCount()
        );
    }

    @Override
    @Transactional
    public void deletePost(UUID postId) {
        Post post = getPost(postId);
        postRepository.delete(post);
        postStatisticsService.invalidate(postId);
    }

    private Post getPost(UUID postId) {
        return postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private PagedResponse<AdminPostResponse> toAdminPostPage(Page<Post> posts) {
        List<UUID> postIds = posts.getContent().stream().map(Post::getId).toList();
        Map<UUID, PostStatistics> statisticsByPostId = postStatisticsService.findAll(postIds);
        Page<AdminPostResponse> page = posts.map(post -> {
            PostStatistics statistics = statisticsByPostId.getOrDefault(
                    post.getId(),
                    new PostStatistics(0, 0, 0)
            );
            return AdminPostResponse.from(
                    post,
                    statistics.likeCount(),
                    statistics.commentCount(),
                    statistics.shareCount()
            );
        });
        return toPagedResponse(page);
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return query.trim();
    }

    private static <T> PagedResponse<T> toPagedResponse(Page<T> page) {
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
}
