package com.cybersocial.message.dto;

import com.cybersocial.message.Message;
import com.cybersocial.message.MessageType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        MessageParticipantResponse sender,
        MessageType messageType,
        String content,
        String mediaUrl,
        String linkUrl,
        List<MessageReactionResponse> reactions,
        Instant createdAt,
        Instant updatedAt
) {
    public static MessageResponse from(Message message, List<MessageReactionResponse> reactions) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                MessageParticipantResponse.from(message.getSender()),
                message.getMessageType(),
                message.getContent(),
                message.getMediaUrl(),
                message.getLinkUrl(),
                reactions,
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }
}
