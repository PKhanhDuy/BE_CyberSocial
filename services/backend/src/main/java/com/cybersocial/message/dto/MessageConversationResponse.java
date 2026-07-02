package com.cybersocial.message.dto;

import com.cybersocial.message.Message;
import com.cybersocial.message.MessageConversation;
import com.cybersocial.user.User;
import java.time.Instant;
import java.util.UUID;

public record MessageConversationResponse(
        UUID id,
        MessageParticipantResponse friend,
        MessageResponse latestMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static MessageConversationResponse from(
            MessageConversation conversation,
            UUID currentUserId,
            Message latestMessage
    ) {
        User friend = conversation.getUserOne().getId().equals(currentUserId)
                ? conversation.getUserTwo()
                : conversation.getUserOne();
        return new MessageConversationResponse(
                conversation.getId(),
                MessageParticipantResponse.from(friend),
                latestMessage == null ? null : MessageResponse.from(latestMessage, java.util.List.of()),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
