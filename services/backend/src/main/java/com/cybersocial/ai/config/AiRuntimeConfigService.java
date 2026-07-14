package com.cybersocial.ai.config;

import com.cybersocial.ai.AiAnalysisProperties;
import com.cybersocial.ai.config.dto.AiConfigResponse;
import com.cybersocial.ai.config.dto.UpdateAiConfigRequest;
import com.cybersocial.common.exception.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguồn cấu hình runtime duy nhất cho pipeline phát hiện tin giả.
 * Đọc từ bảng ai_config (1 dòng), có cache trong bộ nhớ cho đường đi nóng (mỗi lượt tương tác).
 * Khi chưa có dòng cấu hình, fallback về giá trị mặc định trong AiAnalysisProperties.
 */
@Service
public class AiRuntimeConfigService {

    private final AiConfigRepository repository;
    private final AiAnalysisProperties defaults;

    private volatile Snapshot cache;

    public AiRuntimeConfigService(AiConfigRepository repository, AiAnalysisProperties defaults) {
        this.repository = repository;
        this.defaults = defaults;
    }

    public boolean enabled() {
        return current().enabled;
    }

    public List<Integer> thresholds() {
        return current().thresholds;
    }

    public int debounceMinutes() {
        return current().debounceMinutes;
    }

    public int fakeThresholdPercent() {
        return current().fakeThresholdPercent;
    }

    @Transactional(readOnly = true)
    public AiConfigResponse view() {
        return toResponse(current());
    }

    @Transactional
    public AiConfigResponse update(UpdateAiConfigRequest request, UUID adminUserId) {
        List<Integer> thresholds = normalizeThresholds(request.tierThresholds());

        AiConfig config = repository.findById(AiConfig.SINGLETON_ID)
                .orElseGet(() -> AiConfig.builder().id(AiConfig.SINGLETON_ID).build());
        config.setAnalysisEnabled(request.enabled());
        config.setTierThresholds(join(thresholds));
        config.setDebounceMinutes(request.debounceMinutes());
        config.setFakeThresholdPercent(request.fakeThresholdPercent());
        config.setUpdatedBy(adminUserId);
        AiConfig saved = repository.save(config);

        Snapshot snapshot = fromEntity(saved);
        cache = snapshot;
        return toResponse(snapshot);
    }

    private Snapshot current() {
        Snapshot snapshot = cache;
        if (snapshot == null) {
            snapshot = load();
            cache = snapshot;
        }
        return snapshot;
    }

    @Transactional(readOnly = true)
    protected Snapshot load() {
        return repository.findById(AiConfig.SINGLETON_ID)
                .map(this::fromEntity)
                .orElseGet(this::fromDefaults);
    }

    private Snapshot fromEntity(AiConfig config) {
        List<Integer> thresholds = parseThresholds(config.getTierThresholds());
        if (thresholds.isEmpty()) {
            thresholds = defaultThresholds();
        }
        return new Snapshot(
                config.isAnalysisEnabled(),
                thresholds,
                config.getDebounceMinutes(),
                config.getFakeThresholdPercent(),
                config.getUpdatedAt()
        );
    }

    private Snapshot fromDefaults() {
        return new Snapshot(
                defaults.enabled(),
                defaultThresholds(),
                defaults.debounceMinutes(),
                59,
                null
        );
    }

    private List<Integer> defaultThresholds() {
        return new ArrayList<>(defaults.thresholds());
    }

    private List<Integer> normalizeThresholds(List<Integer> thresholds) {
        List<Integer> cleaned = thresholds.stream()
                .filter(value -> value != null && value > 0)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (cleaned.isEmpty()) {
            throw new BadRequestException("Cần ít nhất một mốc tương tác hợp lệ (số nguyên dương).");
        }
        return cleaned;
    }

    private static List<Integer> parseThresholds(String csv) {
        List<Integer> values = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return values;
        }
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                int value = Integer.parseInt(trimmed);
                if (value > 0) {
                    values.add(value);
                }
            } catch (NumberFormatException ignored) {
                // Bỏ qua phần tử không hợp lệ.
            }
        }
        return values;
    }

    private static String join(List<Integer> thresholds) {
        return thresholds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private AiConfigResponse toResponse(Snapshot snapshot) {
        return new AiConfigResponse(
                snapshot.enabled,
                snapshot.thresholds,
                snapshot.debounceMinutes,
                snapshot.fakeThresholdPercent,
                defaults.maxTiers(),
                defaults.timeoutSeconds(),
                defaults.includeSyntheticPosts(),
                snapshot.updatedAt
        );
    }

    private record Snapshot(
            boolean enabled,
            List<Integer> thresholds,
            int debounceMinutes,
            int fakeThresholdPercent,
            java.time.Instant updatedAt
    ) {
    }
}
