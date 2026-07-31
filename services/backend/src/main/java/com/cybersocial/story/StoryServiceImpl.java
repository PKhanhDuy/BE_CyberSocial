package com.cybersocial.story;

import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ForbiddenOperationException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.follow.FollowRepository;
import com.cybersocial.friend.FriendshipRepository;
import com.cybersocial.friend.FriendshipStatus;
import com.cybersocial.notification.NotificationService;
import com.cybersocial.notification.NotificationType;
import com.cybersocial.story.dto.MusicTrackResponse;
import com.cybersocial.story.dto.StoryCreateRequest;
import com.cybersocial.story.dto.StoryMediaRequest;
import com.cybersocial.story.dto.StoryReactionRequest;
import com.cybersocial.story.dto.StoryReactionResponse;
import com.cybersocial.story.dto.StoryReactionSummaryResponse;
import com.cybersocial.story.dto.StoryResponse;
import com.cybersocial.story.dto.StoryViewerResponse;
import com.cybersocial.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryServiceImpl implements StoryService {

    private static final int DEFAULT_IMAGE_STORY_DURATION_MS = 20_000;
    private static final int DEFAULT_MUSIC_DURATION_MS = 20_000;

    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final StoryReactionRepository storyReactionRepository;
    private final MusicTrackRepository musicTrackRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FollowRepository followRepository;
    private final NotificationService notificationService;

    public StoryServiceImpl(
            StoryRepository storyRepository,
            StoryViewRepository storyViewRepository,
            StoryReactionRepository storyReactionRepository,
            MusicTrackRepository musicTrackRepository,
            UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            FollowRepository followRepository,
            NotificationService notificationService
    ) {
        this.storyRepository = storyRepository;
        this.storyViewRepository = storyViewRepository;
        this.storyReactionRepository = storyReactionRepository;
        this.musicTrackRepository = musicTrackRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.followRepository = followRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StoryResponse> findVisibleStories(UUID currentUserId, Pageable pageable) {
        Page<StoryResponse> page = storyRepository.findVisibleStories(currentUserId, Instant.now(), pageable)
                .map(story -> toResponse(story, currentUserId));
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
    public StoryResponse findStory(UUID currentUserId, UUID storyId) {
        Story story = getVisibleStory(currentUserId, storyId);
        return toResponse(story, currentUserId);
    }

    @Override
    @Transactional
    public StoryResponse createStory(UUID currentUserId, StoryCreateRequest request) {
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        StoryMediaRequest mediaRequest = request.media();
        String mediaUrl = normalizeRequired(mediaRequest.mediaUrl(), "Media URL is required");
        String caption = normalizeOptional(request.caption());
        StoryVisibility visibility = request.visibility() == null ? StoryVisibility.FRIENDS : request.visibility();
        MusicTrack musicTrack = resolveMusicTrack(request, mediaRequest);
        Integer musicStartMs = resolveMusicStartMs(musicTrack, request.musicStartMs());
        Integer musicDurationMs = resolveMusicDurationMs(musicTrack, request.musicDurationMs());
        validateMusicWindow(musicTrack, musicStartMs, musicDurationMs);

        Story story = Story.builder()
                .author(author)
                .caption(caption)
                .visibility(visibility)
                .musicTrack(musicTrack)
                .musicStartMs(musicStartMs)
                .musicDurationMs(musicDurationMs)
                .build();

        StoryMedia media = StoryMedia.builder()
                .story(story)
                .mediaType(mediaRequest.mediaType())
                .mediaUrl(mediaUrl)
                .thumbnailUrl(normalizeOptional(mediaRequest.thumbnailUrl()))
                .width(mediaRequest.width())
                .height(mediaRequest.height())
                .durationMs(resolveMediaDurationMs(mediaRequest))
                .build();
        story.setMedia(media);

        Story saved = storyRepository.save(story);
        return toResponse(saved, currentUserId);
    }

    @Override
    @Transactional
    public void deleteStory(UUID currentUserId, UUID storyId) {
        Story story = getStory(storyId);
        if (!story.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You can only delete your own stories");
        }
        story.setArchivedAt(Instant.now());
    }

    @Override
    @Transactional
    public StoryResponse markViewed(UUID currentUserId, UUID storyId) {
        Story story = getVisibleStory(currentUserId, storyId);
        if (!story.getAuthor().getId().equals(currentUserId)
                && !storyViewRepository.existsByStoryIdAndViewerId(storyId, currentUserId)) {
            User viewer = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            storyViewRepository.save(StoryView.builder()
                    .story(story)
                    .viewer(viewer)
                    .build());
        }
        return toResponse(story, currentUserId);
    }

    @Override
    @Transactional
    public StoryReactionResponse react(UUID currentUserId, UUID storyId, StoryReactionRequest request) {
        Story story = getVisibleStory(currentUserId, storyId);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String reactionType = normalizeRequired(request.reactionType(), "Reaction type is required");

        boolean hadReaction = storyReactionRepository.findByStoryIdAndUserId(storyId, currentUserId).isPresent();
        StoryReaction reaction = storyReactionRepository.findByStoryIdAndUserId(storyId, currentUserId)
                .orElseGet(() -> StoryReaction.builder()
                        .story(story)
                        .user(user)
                        .build());
        reaction.setReactionType(reactionType);
        StoryReaction savedReaction = storyReactionRepository.save(reaction);
        notifyStoryReactionOwner(story, user, reactionType, hadReaction);
        return StoryReactionResponse.from(savedReaction);
    }

    @Override
    @Transactional
    public void deleteReaction(UUID currentUserId, UUID storyId) {
        getVisibleStory(currentUserId, storyId);
        storyReactionRepository.findByStoryIdAndUserId(storyId, currentUserId)
                .ifPresent(storyReactionRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MusicTrackResponse> findMusicTracks(String query) {
        String normalizedQuery = normalizeOptional(query);
        List<MusicTrack> tracks = normalizedQuery == null
                ? musicTrackRepository.findByActiveTrueOrderByTitleAsc()
                : musicTrackRepository.findByActiveTrueAndTitleContainingIgnoreCaseOrderByTitleAsc(normalizedQuery);
        return tracks.stream().map(MusicTrackResponse::from).toList();
    }

    private MusicTrack resolveMusicTrack(StoryCreateRequest request, StoryMediaRequest mediaRequest) {
        if (request.musicTrackId() == null) {
            return null;
        }
        if (mediaRequest.mediaType() != StoryMediaType.IMAGE) {
            throw new BadRequestException("Music can only be attached to image stories");
        }
        MusicTrack track = musicTrackRepository.findById(request.musicTrackId())
                .orElseThrow(() -> new ResourceNotFoundException("Music track not found"));
        if (!track.isActive()) {
            throw new BadRequestException("Music track is not available");
        }
        return track;
    }

    private Integer resolveMediaDurationMs(StoryMediaRequest mediaRequest) {
        if (mediaRequest.mediaType() == StoryMediaType.IMAGE && mediaRequest.durationMs() == null) {
            return DEFAULT_IMAGE_STORY_DURATION_MS;
        }
        return mediaRequest.durationMs();
    }

    private Integer resolveMusicStartMs(MusicTrack musicTrack, Integer musicStartMs) {
        if (musicTrack == null) {
            return null;
        }
        return musicStartMs == null ? 0 : musicStartMs;
    }

    private Integer resolveMusicDurationMs(MusicTrack musicTrack, Integer musicDurationMs) {
        if (musicTrack == null) {
            return null;
        }
        return musicDurationMs == null ? DEFAULT_MUSIC_DURATION_MS : musicDurationMs;
    }

    private void validateMusicWindow(MusicTrack musicTrack, Integer musicStartMs, Integer musicDurationMs) {
        if (musicTrack == null) {
            return;
        }

        long musicEndMs = (long) musicStartMs + musicDurationMs;
        long trackDurationMs = musicTrack.getDurationSeconds() * 1_000L;
        if (musicEndMs > trackDurationMs) {
            throw new BadRequestException("Music segment exceeds track duration");
        }
    }

    private Story getVisibleStory(UUID currentUserId, UUID storyId) {
        Story story = getStory(storyId);
        if (story.getArchivedAt() != null || !story.getExpiresAt().isAfter(Instant.now()) || !canViewStory(currentUserId, story)) {
            throw new ResourceNotFoundException("Story not found");
        }
        return story;
    }

    private Story getStory(UUID storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
    }

    private boolean canViewStory(UUID currentUserId, Story story) {
        if (story.getAuthor().getId().equals(currentUserId)) {
            return true;
        }
        if (story.getVisibility() == StoryVisibility.PRIVATE) {
            return false;
        }
        if (isAcceptedFriend(currentUserId, story.getAuthor().getId())) {
            return true;
        }
        return followRepository.existsByFollowerIdAndFollowingId(currentUserId, story.getAuthor().getId());
    }

    private boolean isAcceptedFriend(UUID currentUserId, UUID authorId) {
        return friendshipRepository.findBetween(currentUserId, authorId)
                .map(friendship -> FriendshipStatus.ACCEPTED.equals(friendship.getStatus()))
                .orElse(false);
    }

    private void notifyStoryReactionOwner(Story story, User reactor, String reactionType, boolean hadReaction) {
        if (hadReaction || story.getAuthor().getId().equals(reactor.getId())) {
            return;
        }
        notificationService.create(
                story.getAuthor(),
                NotificationType.STORY,
                "Story của bạn có reaction mới",
                reactor.getDisplayName() + " đã thả " + reactionType + " vào story của bạn."
        );
    }

    private StoryResponse toResponse(Story story, UUID currentUserId) {
        String currentUserReaction = storyReactionRepository.findByStoryIdAndUserId(story.getId(), currentUserId)
                .map(StoryReaction::getReactionType)
                .orElse(null);
        boolean isOwner = story.getAuthor().getId().equals(currentUserId);
        List<StoryViewerResponse> viewers = isOwner
                ? storyViewRepository.findByStoryIdWithViewerOrderByViewedAtDesc(story.getId())
                .stream()
                .map(StoryViewerResponse::from)
                .toList()
                : List.of();
        List<StoryReactionSummaryResponse> reactions = isOwner
                ? storyReactionRepository.findByStoryIdWithUserOrderByCreatedAtDesc(story.getId())
                .stream()
                .map(StoryReactionSummaryResponse::from)
                .toList()
                : List.of();
        return StoryResponse.from(
                story,
                storyViewRepository.countByStoryId(story.getId()),
                storyReactionRepository.countByStoryId(story.getId()),
                storyViewRepository.existsByStoryIdAndViewerId(story.getId(), currentUserId),
                currentUserReaction,
                viewers,
                reactions
        );
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
