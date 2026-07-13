package com.cybersocial.admin.audit;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi nhật ký mọi thao tác quản trị (khóa tài khoản, ẩn/xóa bài, xử lý tin giả, đổi cấu hình AI).
 * Chạy chung transaction với hành động gọi nó, nên log và hành động cùng commit/rollback.
 */
@Service
public class AdminAuditService {

    private static final int MAX_LEN = 500;

    private final AdminActionLogRepository repository;

    public AdminAuditService(AdminActionLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(
            UUID adminUserId,
            AdminActionType actionType,
            AdminTargetType targetType,
            UUID targetId,
            String reason,
            String note
    ) {
        repository.save(AdminActionLog.builder()
                .adminUserId(adminUserId)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .reason(clean(reason))
                .note(clean(note))
                .build());
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= MAX_LEN ? trimmed : trimmed.substring(0, MAX_LEN);
    }
}
