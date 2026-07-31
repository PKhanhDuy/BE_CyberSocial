package com.cybersocial.message.websocket;

import com.cybersocial.message.dto.MessageReactionResponse;
import com.cybersocial.message.dto.MessageResponse;
import java.util.UUID;

public record MessageSocketEvent(
        MessageSocketEventType type,
        UUID conversationId,
        MessageResponse message,
        UUID messageId,
        MessageReactionResponse reaction,
        UUID userId,
        Boolean online
) {
    public static MessageSocketEvent messageCreated(MessageResponse message) {
        return new MessageSocketEvent(
                MessageSocketEventType.MESSAGE_CREATED,
                message.conversationId(),
                message,
                message.id(),
                null,
                null,
                null
        );
    }

    public static MessageSocketEvent reactionUpdated(UUID conversationId, MessageReactionResponse reaction) {
        return new MessageSocketEvent(
                MessageSocketEventType.REACTION_UPDATED,
                conversationId,
                null,
                reaction.messageId(),
                reaction,
                reaction.userId(),
                null
        );
    }

    public static MessageSocketEvent reactionDeleted(UUID conversationId, UUID messageId, UUID userId) {
        return new MessageSocketEvent(
                MessageSocketEventType.REACTION_DELETED,
                conversationId,
                null,
                messageId,
                null,
                userId,
                null
        );
    }

    public static MessageSocketEvent presenceUpdated(UUID userId, boolean online) {
        return new MessageSocketEvent(
                MessageSocketEventType.PRESENCE_UPDATED,
                null,
                null,
                null,
                null,
                userId,
                online
        );
    }
}
