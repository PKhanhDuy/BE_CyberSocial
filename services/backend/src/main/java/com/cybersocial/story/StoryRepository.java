package com.cybersocial.story;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    @EntityGraph(attributePaths = {"author", "media", "musicTrack"})
    @Query("""
            select s from Story s
            where s.archivedAt is null
              and s.expiresAt > :now
              and (
                    s.author.id = :currentUserId
                 or (
                    s.visibility <> com.cybersocial.story.StoryVisibility.PRIVATE
                    and (
                        exists (
                            select f.id from Friendship f
                            where f.status = com.cybersocial.friend.FriendshipStatus.ACCEPTED
                              and (
                                    (f.requester.id = :currentUserId and f.addressee.id = s.author.id)
                                 or (f.addressee.id = :currentUserId and f.requester.id = s.author.id)
                              )
                        )
                        or exists (
                            select uf.id from UserFollow uf
                            where uf.follower.id = :currentUserId
                              and uf.following.id = s.author.id
                        )
                    )
                 )
              )
            order by s.createdAt desc
            """)
    Page<Story> findVisibleStories(
            @Param("currentUserId") UUID currentUserId,
            @Param("now") Instant now,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"author", "media", "musicTrack"})
    Optional<Story> findById(UUID id);

    @Query("""
            select count(s) from Story s
            where s.archivedAt is null
              and s.expiresAt > :now
            """)
    long countActiveStories(@Param("now") java.time.Instant now);
}
