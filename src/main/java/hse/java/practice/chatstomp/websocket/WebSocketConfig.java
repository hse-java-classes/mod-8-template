package hse.java.practice.chatstomp.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Spring конфигурация для WebSocket.
 *
 * @Configuration — Spring понимает, что это конфигурационный класс
 * @EnableWebSocket — активирует поддержку WebSocket в приложении
 *
 * registerWebSocketHandlers — регистрирует обработчик на определённом пути.
 * Клиент подключается к ws://localhost:8080/ws/chat
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(), "/ws/chat")
            .setAllowedOrigins("*");  // В продакшене ограничить до конкретного origin
    }
}
