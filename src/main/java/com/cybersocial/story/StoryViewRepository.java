package com.cybersocial.story;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryViewRepository extends JpaRepository<StoryView, UUID> {

    boolean existsByStoryIdAndViewerId(UUID storyId, UUID viewerId);

    long countByStoryId(UUID storyId);

    @Query("""
            select storyView from StoryView storyView
            join fetch storyView.viewer
            where storyView.story.id = :storyId
            order by storyView.viewedAt desc
            """)
    List<StoryView> findByStoryIdWithViewerOrderByViewedAtDesc(@Param("storyId") UUID storyId);
}
