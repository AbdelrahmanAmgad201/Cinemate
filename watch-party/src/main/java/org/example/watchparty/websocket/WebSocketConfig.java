package org.example.watchparty.websocket;

import lombok.RequiredArgsConstructor;
import org.example.watchparty.redis.RedisSubscriber;
import org.example.watchparty.security.StompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    // Comma-separated list of allowed origins (SEC-NEW-02) — a cross-origin page could
    // otherwise open this socket; this is the transport-level origin gate, distinct from
    // the per-frame authN/authZ enforced by StompAuthChannelInterceptor (REL-08).
    @Value("${app.cors.allowed-origins}")
    private String corsAllowedOrigins;

    // Authenticate CONNECT and authorize SUBSCRIBE/SEND before any frame reaches a
    // controller (REL-08).
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = corsAllowedOrigins.split(",");
        for (int i = 0; i < allowedOrigins.length; i++) {
            allowedOrigins[i] = allowedOrigins[i].trim();
        }

        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Bean
    RedisMessageListenerContainer container(
            RedisConnectionFactory factory,
            RedisSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener((MessageListener) subscriber, new PatternTopic("party:*"));
        return container;
    }
}