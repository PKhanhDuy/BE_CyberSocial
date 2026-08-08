package com.cybersocial.ai;

import com.cybersocial.ai.dto.AIAnalysisRequest;
import com.cybersocial.ai.dto.AIAnalysisResponse;
import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.util.SecurityUtils;
import com.cybersocial.post.Post;
import com.cybersocial.post.PostAccessService;
import com.cybersocial.post.PostRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AIAnalysisController {

    private final AIAnalysisService analysisService;
    private final PostRepository postRepository;
    private final PostAccessService postAccessService;

    public AIAnalysisController(
            AIAnalysisService analysisService,
            PostRepository postRepository,
            PostAccessService postAccessService
    ) {
        this.analysisService = analysisService;
        this.postRepository = postRepository;
        this.postAccessService = postAccessService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<AIAnalysisResponse>> analyze(@Valid @RequestBody AIAnalysisRequest request) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        if (request.postId() != null) {
            Post post = postRepository.findById(request.postId())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
            postAccessService.assertCanView(currentUserId, post);
        }
        return ResponseEntity.ok(ApiResponse.success("Analysis completed", analysisService.analyze(request)));
    }
}
