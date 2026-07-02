package com.cybersocial.story;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryMediaRepository extends JpaRepository<StoryMedia, UUID> {
}
