package com.cybersocial.story;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicTrackRepository extends JpaRepository<MusicTrack, UUID> {

    List<MusicTrack> findByActiveTrueOrderByTitleAsc();

    List<MusicTrack> findByActiveTrueAndTitleContainingIgnoreCaseOrderByTitleAsc(String title);
}
