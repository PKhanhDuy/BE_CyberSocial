package com.cybersocial.message.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class MessageSocketSessionRegistry {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public MessageSocketSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @return true if this is the user's first active session (became online)
     */
    public boolean register(UUID userId, WebSocketSession session) {
        boolean[] becameOnline = {false};
        sessionsByUser.compute(userId, (ignored, existing) -> {
            Set<WebSocketSession> sessions = existing == null ? ConcurrentHashMap.newKeySet() : existing;
            if (sessions.isEmpty()) {
                becameOnline[0] = true;
            }
            sessions.add(session);
            return sessions;
        });
        return becameOnline[0];
    }

    /**
     * @return true if the user has no remaining sessions (became offline)
     */
    public boolean unregister(UUID userId, WebSocketSession session) {
        boolean[] becameOffline = {false};
        sessionsByUser.compute(userId, (ignored, existing) -> {
            if (existing == null) {
                becameOffline[0] = true;
                return null;
            }
            existing.remove(session);
            if (existing.isEmpty()) {
                becameOffline[0] = true;
                return null;
            }
            return existing;
        });
        return becameOffline[0];
    }

    public boolean isOnline(UUID userId) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public Set<UUID> getOnlineUserIds() {
        return sessionsByUser.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void sendToUser(UUID userId, MessageSocketEvent event) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage message = toTextMessage(event);
        sessions.removeIf(session -> !send(session, message));
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    public void sendToUsers(Iterable<UUID> userIds, MessageSocketEvent event) {
        for (UUID userId : userIds) {
            sendToUser(userId, event);
        }
    }

    private TextMessage toTextMessage(MessageSocketEvent event) {
        try {
            return new TextMessage(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize message socket event", exception);
        }
    }

    private boolean send(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            return false;
        }
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
            return true;
        } catch (IOException exception) {
            try {
                session.close();
            } catch (IOException ignored) {
                // Session is already unusable; registry cleanup continues below.
            }
            return false;
        }
    }
}
