package com.cybersocial.notification;

import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.notification.dto.NotificationResponse;
import com.cybersocial.user.User;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    PagedResponse<NotificationResponse> findForUser(UUID currentUserId, Pageable pageable);

    NotificationResponse markAsRead(UUID currentUserId, UUID notificationId);

    void create(User recipient, NotificationType type, String title, String message);
}
