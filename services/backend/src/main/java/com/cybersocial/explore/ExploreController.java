package com.cybersocial.explore;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.explore.dto.ExploreOverviewResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/explore")
public class ExploreController {

    private final ExploreService exploreService;

    public ExploreController(ExploreService exploreService) {
        this.exploreService = exploreService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<ExploreOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success("Explore overview loaded", exploreService.getOverview()));
    }
}
