package com.cybersocial.post;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {

    long countByPostId(UUID postId);

    @Query("""
            select comment from PostComment comment
            join fetch comment.user
            where comment.post.id = :postId
            order by comment.createdAt asc
            """)
    List<PostComment> findByPostIdWithUserOrderByCreatedAtAsc(@Param("postId") UUID postId);
}
