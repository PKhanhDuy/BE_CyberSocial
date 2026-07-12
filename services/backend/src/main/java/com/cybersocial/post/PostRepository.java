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
                      and post.hidden = false
                    order by post.createdAt desc
                    """,
            countQuery = """
                    select count(post) from Post post
                    where post.synthetic = false
                      and post.hidden = false
                    """
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
                      and post.hidden = false
                    order by post.createdAt desc
                    """,
            countQuery = """
                    select count(post) from Post post
                    where post.author.id = :authorId
                      and post.synthetic = false
                      and post.hidden = false
                    """
    )
    Page<Post> findVisiblePostsByAuthor(@Param("authorId") UUID authorId, Pageable pageable);

    long countByAuthorId(UUID authorId);

    @Query("""
            select post from Post post
            join fetch post.author
            where post.id = :postId
            """)
    java.util.Optional<Post> findByIdWithAuthor(@Param("postId") UUID postId);

    @Query("""
            select post from Post post
            join fetch post.author
            left join fetch post.sharedPost sharedPost
            left join fetch sharedPost.author
            where post.id = :postId
            """)
    java.util.Optional<Post> findByIdWithSharedPost(@Param("postId") UUID postId);

    @Query("""
            select count(post) from Post post
            where post.synthetic = false
              and post.hidden = false
            """)
    long countVisiblePosts();

    long countByHiddenTrue();

    long countBySyntheticFalse();

    @Query(
            value = """
                    select post from Post post
                    join fetch post.author
                    left join fetch post.sharedPost sharedPost
                    left join fetch sharedPost.author
                    where post.synthetic = false
                      and (:hidden is null or post.hidden = :hidden)
                      and (:query = ''
                        or lower(post.content) like concat('%', lower(:query), '%')
                        or lower(post.author.displayName) like concat('%', lower(:query), '%'))
                    order by post.createdAt desc
                    """,
            countQuery = """
                    select count(post) from Post post
                    join post.author author
                    where post.synthetic = false
                      and (:hidden is null or post.hidden = :hidden)
                      and (:query = ''
                        or lower(post.content) like concat('%', lower(:query), '%')
                        or lower(author.displayName) like concat('%', lower(:query), '%'))
                    """
    )
    Page<Post> findForAdmin(
            @Param("query") String query,
            @Param("hidden") Boolean hidden,
            Pageable pageable
    );

    @Query(
            value = """
                    select post from Post post
                    join fetch post.author
                    join PostVerification verification on verification.post = post
                    where post.synthetic = false
                      and verification.verificationStatus = com.cybersocial.post.PostVerificationStatus.COMPLETED
                      and verification.label = 'FAKE'
                    order by verification.updatedAt desc
                    """,
            countQuery = """
                    select count(post) from Post post
                    join PostVerification verification on verification.post = post
                    where post.synthetic = false
                      and verification.verificationStatus = com.cybersocial.post.PostVerificationStatus.COMPLETED
                      and verification.label = 'FAKE'
                    """
    )
    Page<Post> findFakePosts(Pageable pageable);

    @Query(
            value = """
                    select post from Post post
                    join fetch post.author
                    left join fetch post.sharedPost sharedPost
                    left join fetch sharedPost.author
                    join PostVerification verification on verification.post = post
                    where post.synthetic = false
                      and post.hidden = false
                      and verification.verificationStatus = com.cybersocial.post.PostVerificationStatus.COMPLETED
                      and verification.label = 'REAL'
                    order by verification.updatedAt desc
                    """,
            countQuery = """
                    select count(post) from Post post
                    join PostVerification verification on verification.post = post
                    where post.synthetic = false
                      and post.hidden = false
                      and verification.verificationStatus = com.cybersocial.post.PostVerificationStatus.COMPLETED
                      and verification.label = 'REAL'
                    """
    )
    Page<Post> findVerifiedRealPosts(Pageable pageable);

    @Query("""
            select post.content from Post post
            where post.synthetic = false
              and post.hidden = false
              and post.content is not null
              and post.content <> ''
            order by post.createdAt desc
            """)
    java.util.List<String> findRecentVisibleContents(Pageable pageable);
}
