package com.cybersocial.notification;

import com.cybersocial.common.exception.ForbiddenOperationException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.notification.dto.NotificationResponse;
import com.cybersocial.user.User;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> findForUser(UUID currentUserId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(currentUserId, pageable)
                .map(NotificationResponse::from);
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID currentUserId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getRecipient().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("Notification belongs to another user");
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public void create(User recipient, NotificationType type, String title, String message) {
        notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .build());
    }
}
