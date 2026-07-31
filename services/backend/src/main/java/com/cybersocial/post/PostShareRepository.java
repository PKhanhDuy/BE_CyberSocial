package com.cybersocial.post;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostShareRepository extends JpaRepository<PostShare, UUID> {

    long countByPostId(UUID postId);

    @Query("""
            select postShare.post.id as postId, count(postShare) as count
            from PostShare postShare
            where postShare.post.id in :postIds
            group by postShare.post.id
            """)
    List<PostCountProjection> countByPostIds(@Param("postIds") Collection<UUID> postIds);

    @Query("""
            select postShare from PostShare postShare
            join fetch postShare.user
            left join fetch postShare.parentShare
            where postShare.post.id = :postId
            order by postShare.createdAt asc
            """)
    List<PostShare> findByPostIdWithUserOrderByCreatedAtAsc(@Param("postId") UUID postId);

    Optional<PostShare> findFirstByPostIdAndUserIdOrderByCreatedAtDesc(UUID postId, UUID userId);

    Optional<PostShare> findByRepostPostId(UUID repostPostId);
}
