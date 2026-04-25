package hse.java.practice.chatstomp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Простой WebSocket обработчик для чата.
 *
 * Как это работает:
 * 1. Клиент подключается к WebSocket (обработчик получает afterConnectionEstablished)
 * 2. Клиент отправляет JSON сообщение (handleTextMessage)
 * 3. Сервер добавляет timestamp и рассылает всем подключённым клиентам
 * 4. При отключении клиента удаляем из списка (afterConnectionClosed)
 *
 * Разница с STOMP:
 * - WebSocket: низкоуровневый транспорт, сами управляем сессиями
 * - STOMP: протокол поверх WebSocket, Spring управляет подписками и топиками
 *
 * В чат-stomp используется STOMP, но суть та же:
 * - в /app/send отправляем сообщение
 * - в /topic/messages подписываемся на рассылку
 *
 * Потокобезопасность:
 * CopyOnWriteArraySet автоматически синхронизирует доступ к списку сессий.
 */
@Component
class ChatWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("Client connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println(payload);

        session.sendMessage(message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("Client disconnected: " + session.getId());
    }

    record ChatMessage(String sender, String text, Long timestamp) {}
}
