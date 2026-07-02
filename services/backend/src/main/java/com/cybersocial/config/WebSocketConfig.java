package com.cybersocial.config;

import com.cybersocial.message.websocket.MessageSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageSocketHandler messageSocketHandler;
    private final CorsProperties corsProperties;

    public WebSocketConfig(MessageSocketHandler messageSocketHandler, CorsProperties corsProperties) {
        this.messageSocketHandler = messageSocketHandler;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageSocketHandler, "/ws/messages")
                .setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new));
    }
}
