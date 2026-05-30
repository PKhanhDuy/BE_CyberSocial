package com.cybersocial.story;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "story_media")
@EntityListeners(AuditingEntityListener.class)
public class StoryMedia {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false, unique = true)
    private Story story;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoryMediaType mediaType;

    @Column(nullable = false, length = 2048)
    private String mediaUrl;

    @Column(length = 2048)
    private String thumbnailUrl;

    private Integer width;

    private Integer height;

    private Integer durationMs;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
