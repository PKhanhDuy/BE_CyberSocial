package com.cybersocial.post;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @Query(
            value = """
                    select post from Post post
                    join fetch post.author
                    left join fetch post.sharedPost sharedPost
                    left join fetch sharedPost.author
                    where post.synthetic = false
                    order by post.createdAt desc
                    """,
            countQuery = "select count(post) from Post post where post.synthetic = false"
    )
    Page<Post> findVisiblePosts(Pageable pageable);

    @Query(
            value = """
                    select post from Post post
                    join fetch post.author
                    left join fetch post.sharedPost sharedPost
                    left join fetch sharedPost.author
                    where post.author.id = :authorId
                      and post.synthetic = false
                    order by post.createdAt desc
                    """,
            countQuery = """
                    select count(post) from Post post
                    where post.author.id = :authorId
                      and post.synthetic = false
                    """
    )
    Page<Post> findVisiblePostsByAuthor(@Param("authorId") UUID authorId, Pageable pageable);
}
