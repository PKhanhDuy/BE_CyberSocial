package com.cybersocial.story;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryReactionRepository extends JpaRepository<StoryReaction, UUID> {

    Optional<StoryReaction> findByStoryIdAndUserId(UUID storyId, UUID userId);

    long countByStoryId(UUID storyId);

    @Query("""
            select reaction from StoryReaction reaction
            join fetch reaction.user
            where reaction.story.id = :storyId
            order by reaction.createdAt desc
            """)
    List<StoryReaction> findByStoryIdWithUserOrderByCreatedAtDesc(@Param("storyId") UUID storyId);
}
