package com.cybersocial.admin;

import com.cybersocial.admin.dto.AdminFakePostResponse;
import com.cybersocial.admin.dto.AdminPostResponse;
import com.cybersocial.admin.dto.DeletePostRequest;
import com.cybersocial.admin.dto.PostVerdictRequest;
import com.cybersocial.admin.dto.UpdatePostHiddenRequest;
import com.cybersocial.common.response.PagedResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AdminPostService {

    PagedResponse<AdminPostResponse> listPosts(String query, Boolean hidden, Pageable pageable);

    PagedResponse<AdminFakePostResponse> listFakePosts(Pageable pageable);

    AdminPostResponse updatePostHidden(UUID postId, UpdatePostHiddenRequest request);

    void deletePost(UUID postId, DeletePostRequest request);

    AdminFakePostResponse applyVerdict(UUID postId, PostVerdictRequest request);
}
