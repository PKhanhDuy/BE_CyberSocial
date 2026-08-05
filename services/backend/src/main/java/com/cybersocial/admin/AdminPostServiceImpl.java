package com.cybersocial.admin;

import com.cybersocial.admin.audit.AdminActionType;
import com.cybersocial.admin.audit.AdminAuditService;
import com.cybersocial.admin.audit.AdminTargetType;
import com.cybersocial.admin.dto.AdminFakePostResponse;
import com.cybersocial.admin.dto.AdminPostResponse;
import com.cybersocial.admin.dto.AdminVerdictDecision;
import com.cybersocial.admin.dto.DeletePostRequest;
import com.cybersocial.admin.dto.PostVerdictRequest;
import com.cybersocial.admin.dto.UpdatePostHiddenRequest;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ConflictException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.common.util.SecurityUtils;
import com.cybersocial.notification.NotificationService;
import com.cybersocial.notification.NotificationType;
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
    private final NotificationService notificationService;
    private final AdminAuditService auditService;

    public AdminPostServiceImpl(
            PostRepository postRepository,
            PostVerificationRepository postVerificationRepository,
            PostStatisticsService postStatisticsService,
            NotificationService notificationService,
            AdminAuditService auditService
    ) {
        this.postRepository = postRepository;
        this.postVerificationRepository = postVerificationRepository;
        this.postStatisticsService = postStatisticsService;
        this.notificationService = notificationService;
        this.auditService = auditService;
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
    public PagedResponse<AdminFakePostResponse> listFakePosts(Boolean reviewed, Pageable pageable) {
        Page<Post> posts = postRepository.findFakePosts(reviewed, pageable);
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
        boolean target = request.hidden();

        if (post.isHidden() == target) {
            throw new ConflictException(
                    "Bài viết này đã được " + (target ? "gỡ bỏ" : "khôi phục")
                            + " hoặc xử lý bởi một quản trị viên khác.");
        }
        if (target && (request.reason() == null || request.reason().isBlank())) {
            throw new BadRequestException(
                    "Vui lòng chọn tiêu chuẩn cộng đồng bị vi phạm để tiếp tục gỡ bài viết.");
        }

        post.setHidden(target);
        post.setHiddenAt(target ? Instant.now() : null);

        UUID adminId = SecurityUtils.requireCurrentUserId();
        auditService.record(adminId, target ? AdminActionType.HIDE_POST : AdminActionType.UNHIDE_POST,
                AdminTargetType.POST, postId, request.reason(), null);

        if (target) {
            notifyAuthor(post, "Bài viết của bạn đã bị ẩn",
                    "Bài viết của bạn đã bị quản trị viên ẩn khỏi bảng tin công khai.", request.reason());
        }

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
    public void deletePost(UUID postId, DeletePostRequest request) {
        Post post = getPost(postId);
        UUID adminId = SecurityUtils.requireCurrentUserId();

        // Thông báo tác giả trước khi xóa (author còn được load qua findByIdWithAuthor).
        notifyAuthor(post, "Bài viết của bạn đã bị gỡ bỏ",
                "Bài viết của bạn đã bị quản trị viên gỡ bỏ vĩnh viễn khỏi hệ thống.", request.reason());
        auditService.record(adminId, AdminActionType.DELETE_POST, AdminTargetType.POST,
                postId, request.reason(), null);

        postRepository.delete(post);
        postStatisticsService.invalidate(postId);
    }

    @Override
    @Transactional
    public AdminFakePostResponse applyVerdict(UUID postId, PostVerdictRequest request) {
        Post post = getPost(postId);
        PostVerification verification = postVerificationRepository.findByPostId(postId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa có kết quả phân tích AI cho bài viết này."));

        UUID adminId = SecurityUtils.requireCurrentUserId();
        AdminVerdictDecision decision = request.decision();

        verification.setAdminDecision(decision.name());
        verification.setAdminNote(request.note());
        verification.setReviewedAt(Instant.now());
        verification.setReviewedBy(adminId);

        if (decision == AdminVerdictDecision.CONFIRM_FAKE) {
            // Dán nhãn cảnh báo công khai, giữ nguyên hiển thị bài viết (theo quyết định sản phẩm).
            verification.setPublicLabel(true);
            auditService.record(adminId, AdminActionType.CONFIRM_FAKE, AdminTargetType.POST,
                    postId, request.note(), null);
            notifyAuthor(post, "Bài viết của bạn bị gắn nhãn cảnh báo",
                    "Bài viết của bạn đã được gắn nhãn \"Thông tin chưa kiểm chứng/Sai sự thật\" hiển thị công khai.",
                    request.note());
        } else {
            // Bác bỏ nhãn AI: gỡ nhãn nghi vấn, trả bài về hiển thị bình thường.
            verification.setPublicLabel(false);
            auditService.record(adminId, AdminActionType.REJECT_FAKE_LABEL, AdminTargetType.POST,
                    postId, request.note(), null);
        }

        postVerificationRepository.save(verification);
        return AdminFakePostResponse.from(post, verification);
    }

    private void notifyAuthor(Post post, String title, String body, String reason) {
        if (post.isSynthetic()) {
            return;
        }
        String message = body + (reason == null || reason.isBlank() ? "" : " Lý do: " + reason.trim());
        notificationService.create(post.getAuthor(), NotificationType.SECURITY, title, message);
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
