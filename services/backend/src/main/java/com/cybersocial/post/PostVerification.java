package com.cybersocial.post;

import com.cybersocial.ai.RiskLevel;
import com.cybersocial.ai.dto.EventAttributionResponse;
import com.cybersocial.ai.dto.PropagationTimelineEventResponse;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post_verifications")
@EntityListeners(AuditingEntityListener.class)
public class PostVerification {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private PostVerificationStatus verificationStatus = PostVerificationStatus.PENDING;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(columnDefinition = "text")
    private String interpretation;

    @Column(columnDefinition = "text")
    private String headline;

    @Column(columnDefinition = "text")
    private String narrative;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_hints", columnDefinition = "jsonb")
    private List<String> contextHints = new ArrayList<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_attributions", columnDefinition = "jsonb")
    private List<EventAttributionResponse> eventAttributions = new ArrayList<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "propagation_timeline", columnDefinition = "jsonb")
    private List<PropagationTimelineEventResponse> propagationTimeline = new ArrayList<>();

    @Column(name = "fake_probability", precision = 7, scale = 6)
    private BigDecimal fakeProbability;

    @Column(length = 10)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 10)
    private RiskLevel riskLevel;

    @Column(precision = 7, scale = 6)
    private BigDecimal threshold;

    @Column(length = 30)
    private String mode;

    @Builder.Default
    @Column(name = "analysis_tier", nullable = false)
    private int analysisTier = 0;

    @Column(name = "interaction_count_at_analysis")
    private Integer interactionCountAtAnalysis;

    @Column(name = "last_analyzed_at")
    private Instant lastAnalyzedAt;

    /** Admin đã xác nhận tin giả → dán nhãn cảnh báo công khai trên bài viết. */
    @Builder.Default
    @Column(name = "public_label", nullable = false)
    private boolean publicLabel = false;

    /** Quyết định của admin: CONFIRM_FAKE | REJECT_LABEL (null nếu chưa duyệt). */
    @Column(name = "admin_decision", length = 20)
    private String adminDecision;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public boolean interactionsLocked() {
        if (verificationStatus != PostVerificationStatus.COMPLETED) {
            return false;
        }
        if (!"FAKE".equals(label)) {
            return false;
        }
        return !"REJECT_LABEL".equals(adminDecision);
    }

    /** Nhãn hiển thị cho người dùng: admin bác bỏ thì coi như REAL dù AI vẫn lưu FAKE. */
    public String effectiveLabel() {
        if ("REJECT_LABEL".equals(adminDecision)) {
            return "REAL";
        }
        return label;
    }
}
