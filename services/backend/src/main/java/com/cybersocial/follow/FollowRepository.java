package com.cybersocial.follow;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<UserFollow, UUID> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    Optional<UserFollow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    long countByFollowingId(UUID followingId);

    long countByFollowerId(UUID followerId);

    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    @Query("""
            select uf from UserFollow uf
            join fetch uf.follower
            where uf.following.id = :userId
            order by uf.createdAt desc
            """)
    Page<UserFollow> findFollowersByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            select uf from UserFollow uf
            join fetch uf.following
            where uf.follower.id = :userId
            order by uf.createdAt desc
            """)
    Page<UserFollow> findFollowingByUserId(@Param("userId") UUID userId, Pageable pageable);
}
