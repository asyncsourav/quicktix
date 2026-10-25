

package com.asyncsourav.quicktix.config;



import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;




@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    private final CorsProperties corsProperties;


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        // In-memory message broker for broadcasting seat updates to subscribers
        config.enableSimpleBroker("/topic");

        // Destination prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
    
        String[] allowedOrigins = corsProperties.getAllowedOrigins().toArray(new String[0]);

        // STOMP endpoint with SockJS fallback for web browsers
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();

        // Native WebSocket endpoint for clients without SockJS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
