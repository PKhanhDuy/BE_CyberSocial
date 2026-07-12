package com.cybersocial.post;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    @Query("""
            select postLike.post.id as postId, count(postLike) as count
            from PostLike postLike
            where postLike.post.id in :postIds
            group by postLike.post.id
            """)
    List<PostCountProjection> countByPostIds(@Param("postIds") Collection<UUID> postIds);

    @Query("""
            select postLike.post.id
            from PostLike postLike
            where postLike.user.id = :userId
              and postLike.post.id in :postIds
            """)
    Set<UUID> findLikedPostIds(
            @Param("userId") UUID userId,
            @Param("postIds") Collection<UUID> postIds
    );

    @Query("""
            select postLike from PostLike postLike
            join fetch postLike.user
            where postLike.post.id = :postId
            order by postLike.createdAt asc
            """)
    List<PostLike> findByPostIdWithUserOrderByCreatedAtAsc(@Param("postId") UUID postId);
}
