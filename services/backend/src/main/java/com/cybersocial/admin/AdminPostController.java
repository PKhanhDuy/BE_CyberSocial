package com.cybersocial.admin;

import com.cybersocial.admin.dto.AdminFakePostResponse;
import com.cybersocial.admin.dto.AdminPostResponse;
import com.cybersocial.admin.dto.DeletePostRequest;
import com.cybersocial.admin.dto.PostVerdictRequest;
import com.cybersocial.admin.dto.UpdatePostHiddenRequest;
import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.response.PagedResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final AdminPostService adminPostService;

    public AdminPostController(AdminPostService adminPostService) {
        this.adminPostService = adminPostService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminPostResponse>>> listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean hidden
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                "Admin posts loaded",
                adminPostService.listPosts(query, hidden, pageable)
        ));
    }

    @GetMapping("/fake")
    public ResponseEntity<ApiResponse<PagedResponse<AdminFakePostResponse>>> listFakePosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean reviewed
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizedSize);
        return ResponseEntity.ok(ApiResponse.success(
                "Fake posts loaded",
                adminPostService.listFakePosts(reviewed, pageable)
        ));
    }

    @PatchMapping("/{id}/hidden")
    public ResponseEntity<ApiResponse<AdminPostResponse>> updatePostHidden(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostHiddenRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                request.hidden() ? "Post hidden" : "Post restored",
                adminPostService.updatePostHidden(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID id,
            @Valid @RequestBody DeletePostRequest request
    ) {
        adminPostService.deletePost(id, request);
        return ResponseEntity.ok(ApiResponse.success("Post deleted", null));
    }

    @PostMapping("/{id}/verdict")
    public ResponseEntity<ApiResponse<AdminFakePostResponse>> applyVerdict(
            @PathVariable UUID id,
            @Valid @RequestBody PostVerdictRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã xử lý nhãn tin giả",
                adminPostService.applyVerdict(id, request)
        ));
    }
}
