package com.cybersocial.post;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);
}
