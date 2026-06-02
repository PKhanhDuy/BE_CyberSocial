package com.cybersocial.story.dto;

import com.cybersocial.story.MusicTrack;
import java.util.UUID;

public record MusicTrackResponse(
        UUID id,
        String title,
        String artist,
        String audioUrl,
        String coverUrl,
        Integer durationSeconds
) {
    public static MusicTrackResponse from(MusicTrack track) {
        if (track == null) {
            return null;
        }
        return new MusicTrackResponse(
                track.getId(),
                track.getTitle(),
                track.getArtist(),
                track.getAudioUrl(),
                track.getCoverUrl(),
                track.getDurationSeconds()
        );
    }
}
