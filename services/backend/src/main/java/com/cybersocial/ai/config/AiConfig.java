package com.cybersocial.ai.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Cấu hình runtime của mô-đun phát hiện tin giả. Bảng chỉ có duy nhất 1 dòng (id = 1).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_config")
@EntityListeners(AuditingEntityListener.class)
public class AiConfig {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(name = "analysis_enabled", nullable = false)
    private boolean analysisEnabled;

    /** Các mốc tương tác kích hoạt phân tích lại, lưu dạng CSV, ví dụ "5,15,30". */
    @Column(name = "tier_thresholds", nullable = false, length = 100)
    private String tierThresholds;

    @Column(name = "debounce_minutes", nullable = false)
    private int debounceMinutes;

    /** Ngưỡng gắn nhãn FAKE hiển thị cho admin (0–100%). Ngưỡng thực thi nằm ở AI service. */
    @Column(name = "fake_threshold_percent", nullable = false)
    private int fakeThresholdPercent;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
