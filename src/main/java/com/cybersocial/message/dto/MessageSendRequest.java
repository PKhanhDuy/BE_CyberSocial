package com.cybersocial.message.dto;

import com.cybersocial.message.MessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageSendRequest(
        @NotNull(message = "Message type is required")
        MessageType messageType,

        @Size(max = 4000, message = "Message content must be 4000 characters or fewer")
        String content,

        @Size(max = 2048, message = "Media URL must be 2048 characters or fewer")
        String mediaUrl,

        @Size(max = 2048, message = "Link URL must be 2048 characters or fewer")
        String linkUrl
) {
}
