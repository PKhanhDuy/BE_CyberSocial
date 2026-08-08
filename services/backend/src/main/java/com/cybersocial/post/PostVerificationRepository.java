package com.cybersocial.post;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostVerificationRepository extends JpaRepository<PostVerification, UUID> {

    @Query("""
            select verification from PostVerification verification
            join fetch verification.post
            where verification.post.id = :postId
            """)
    Optional<PostVerification> findByPostId(@Param("postId") UUID postId);

    @Query("""
            select verification from PostVerification verification
            join fetch verification.post post
            join fetch post.author
            where post.id in :postIds
            """)
    java.util.List<PostVerification> findByPostIds(@Param("postIds") java.util.Collection<UUID> postIds);

    @Query("""
            select verification from PostVerification verification
            join fetch verification.post post
            join fetch post.author
            where verification.verificationStatus = :status
              and post.synthetic = false
            order by verification.updatedAt desc
            """)
    java.util.List<PostVerification> findByStatusWithPost(
            @Param("status") PostVerificationStatus status,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
            select verification from PostVerification verification
            join fetch verification.post post
            join fetch post.author
            where verification.verificationStatus in :statuses
              and post.synthetic = false
            order by verification.updatedAt desc
            """)
    java.util.List<PostVerification> findByStatusesWithPost(
            @Param("statuses") java.util.Collection<PostVerificationStatus> statuses,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
            select avg(1 - verification.fakeProbability)
            from PostVerification verification
            join verification.post post
            where verification.verificationStatus = com.cybersocial.post.PostVerificationStatus.COMPLETED
              and verification.fakeProbability is not null
              and post.synthetic = false
            """)
    Double averageRealProbability();

    @Query("""
            select count(verification)
            from PostVerification verification
            join verification.post post
            where verification.verificationStatus = com.cybersocial.post.PostVerificationStatus.COMPLETED
              and verification.label = 'FAKE'
              and post.synthetic = false
            """)
    long countCompletedFakeVerifications();

    @Query("""
            select count(verification)
            from PostVerification verification
            join verification.post post
            where verification.verificationStatus = com.cybersocial.post.PostVerificationStatus.COMPLETED
              and post.synthetic = false
            """)
    long countCompletedVerifications();

    @Query("""
            select count(verification)
            from PostVerification verification
            join verification.post post
            where post.synthetic = false
              and post.hidden = false
              and post.visibility <> com.cybersocial.post.PostVisibility.PRIVATE
              and verification.verificationStatus = com.cybersocial.post.PostVerificationStatus.COMPLETED
              and verification.label = 'REAL'
            """)
    long countVerifiedRealPosts();

    @Query(value = """
            select avg(extract(epoch from (v.last_analyzed_at - p.created_at)) * 1000)
            from post_verifications v
            inner join posts p on p.id = v.post_id
            where p.is_synthetic = false
              and p.hidden = false
              and p.visibility <> 'PRIVATE'
              and v.verification_status = 'COMPLETED'
              and v.label = 'REAL'
              and v.last_analyzed_at is not null
            """, nativeQuery = true)
    Double averageVerifiedAnalysisDelayMs();

    @Query("""
            select count(verification)
            from PostVerification verification
            join verification.post post
            where verification.verificationStatus = :status
              and post.synthetic = false
              and post.hidden = false
              and post.visibility <> com.cybersocial.post.PostVisibility.PRIVATE
            """)
    long countByStatusForVisiblePosts(@Param("status") PostVerificationStatus status);
}
