package com.cybersocial.message.websocket;

import com.cybersocial.security.jwt.JwtUtil;
import java.net.URI;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MessageSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID_ATTRIBUTE = "userId";

    private final JwtUtil jwtUtil;
    private final MessageSocketSessionRegistry sessionRegistry;

    public MessageSocketHandler(JwtUtil jwtUtil, MessageSocketSessionRegistry sessionRegistry) {
        this.jwtUtil = jwtUtil;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID userId = resolveUserId(session.getUri());
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid token"));
            return;
        }

        session.getAttributes().put(USER_ID_ATTRIBUTE, userId);
        sessionRegistry.register(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (userId instanceof UUID id) {
            sessionRegistry.unregister(id, session);
        }
    }

    private UUID resolveUserId(URI uri) {
        if (uri == null) {
            return null;
        }
        String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
        if (token == null || !jwtUtil.isValid(token)) {
            return null;
        }
        return jwtUtil.extractUserId(token);
    }
}
