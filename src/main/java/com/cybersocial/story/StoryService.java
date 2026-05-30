package com.cybersocial.story;

import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.story.dto.MusicTrackResponse;
import com.cybersocial.story.dto.StoryCreateRequest;
import com.cybersocial.story.dto.StoryReactionRequest;
import com.cybersocial.story.dto.StoryReactionResponse;
import com.cybersocial.story.dto.StoryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface StoryService {

    PagedResponse<StoryResponse> findVisibleStories(UUID currentUserId, Pageable pageable);

    StoryResponse findStory(UUID currentUserId, UUID storyId);

    StoryResponse createStory(UUID currentUserId, StoryCreateRequest request);

    void deleteStory(UUID currentUserId, UUID storyId);

    StoryResponse markViewed(UUID currentUserId, UUID storyId);

    StoryReactionResponse react(UUID currentUserId, UUID storyId, StoryReactionRequest request);

    void deleteReaction(UUID currentUserId, UUID storyId);

    List<MusicTrackResponse> findMusicTracks(String query);
}
