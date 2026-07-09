package com.cybersocial.story;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.common.util.SecurityUtils;
import com.cybersocial.story.dto.StoryCreateRequest;
import com.cybersocial.story.dto.StoryReactionRequest;
import com.cybersocial.story.dto.StoryReactionResponse;
import com.cybersocial.story.dto.StoryResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StoryResponse>>> listStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(storyService.findVisibleStories(SecurityUtils.requireCurrentUserId(), pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoryResponse>> getStory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(storyService.findStory(SecurityUtils.requireCurrentUserId(), id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(@Valid @RequestBody StoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Story created", storyService.createStory(SecurityUtils.requireCurrentUserId(), request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStory(@PathVariable UUID id) {
        storyService.deleteStory(SecurityUtils.requireCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Story deleted", null));
    }

    @PostMapping("/{id}/views")
    public ResponseEntity<ApiResponse<StoryResponse>> markViewed(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Story viewed", storyService.markViewed(SecurityUtils.requireCurrentUserId(), id)));
    }

    @PostMapping("/{id}/reactions")
    public ResponseEntity<ApiResponse<StoryReactionResponse>> react(
            @PathVariable UUID id,
            @Valid @RequestBody StoryReactionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Story reaction saved", storyService.react(SecurityUtils.requireCurrentUserId(), id, request)));
    }

    @DeleteMapping("/{id}/reactions")
    public ResponseEntity<ApiResponse<Void>> deleteReaction(@PathVariable UUID id) {
        storyService.deleteReaction(SecurityUtils.requireCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Story reaction deleted", null));
    }
}
