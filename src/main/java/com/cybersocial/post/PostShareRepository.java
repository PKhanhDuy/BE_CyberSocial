package com.cybersocial.post;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostShareRepository extends JpaRepository<PostShare, UUID> {

    long countByPostId(UUID postId);
}
