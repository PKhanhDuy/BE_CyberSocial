package com.cybersocial.post;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {

    long countByPostId(UUID postId);

    @Query("""
            select comment.post.id as postId, count(comment) as count
            from PostComment comment
            where comment.post.id in :postIds
            group by comment.post.id
            """)
    List<PostCountProjection> countByPostIds(@Param("postIds") Collection<UUID> postIds);

    @Query(
            value = """
                    select comment from PostComment comment
                    join fetch comment.user
                    where comment.post.id = :postId
                    order by comment.createdAt asc
                    """,
            countQuery = """
                    select count(comment) from PostComment comment
                    where comment.post.id = :postId
                    """
    )
    Page<PostComment> findByPostIdWithUserOrderByCreatedAtAsc(@Param("postId") UUID postId, Pageable pageable);
}
