package com.cybersocial.post;

import java.util.Collection;
import java.util.List;
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
}
