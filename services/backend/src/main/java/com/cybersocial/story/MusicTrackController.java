package com.cybersocial.story;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.story.dto.MusicTrackResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/music-tracks")
public class MusicTrackController {

    private final StoryService storyService;

    public MusicTrackController(StoryService storyService) {
        this.storyService = storyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MusicTrackResponse>>> listMusicTracks(
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(ApiResponse.success(storyService.findMusicTracks(query)));
    }
}
