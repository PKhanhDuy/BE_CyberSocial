package com.cybersocial.message.websocket;

import com.cybersocial.message.MessageConversation;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class MessageSocketPublisher {

    private final MessageSocketSessionRegistry sessionRegistry;

    public MessageSocketPublisher(MessageSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void publishToParticipants(MessageConversation conversation, MessageSocketEvent event) {
        List<UUID> participantIds = List.of(
                conversation.getUserOne().getId(),
                conversation.getUserTwo().getId()
        );
        publishAfterCommit(participantIds, event);
    }

    private void publishAfterCommit(List<UUID> userIds, MessageSocketEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            userIds.forEach(userId -> sessionRegistry.sendToUser(userId, event));
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                userIds.forEach(userId -> sessionRegistry.sendToUser(userId, event));
            }
        });
    }
}
