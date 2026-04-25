package hse.java.chatstomp;

import hse.java.practice.chatstomp.ChatStompApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * STOMP-взаимодействие. Тег: chat-stomp-2 (2 балла).
 *
 * Проверяет отправку/получение сообщений, поля ответа, timestamp сервера, broadcast.
 */
@Tag("chat-stomp-2")
@SpringBootTest(
    classes = ChatStompApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatStompMessagingTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(
            new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    // -- basic delivery --------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("STOMP: сообщение доходит подписчику")
    void messageDelivered() throws Exception {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingDeque<>();

        StompSession session = connect();
        session.subscribe("/topic/messages", mapHandler(queue));
        session.send("/app/send", Map.of("sender", "Alice", "text", "Hello"));

        Map<String, Object> received = queue.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.get("sender")).isEqualTo("Alice");
        assertThat(received.get("text")).isEqualTo("Hello");
    }

    @Test
    @Order(2)
    @DisplayName("STOMP: ответ содержит поля sender, text, timestamp")
    void responseContainsAllFields() throws Exception {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingDeque<>();

        StompSession session = connect();
        session.subscribe("/topic/messages", mapHandler(queue));
        session.send("/app/send", Map.of("sender", "Bob", "text", "fields check"));

        Map<String, Object> received = queue.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received).containsKeys("sender", "text", "timestamp");
    }

    // -- timestamp -------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("STOMP: timestamp формата HH:mm:ss")
    void timestampFormat() throws Exception {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingDeque<>();

        StompSession session = connect();
        session.subscribe("/topic/messages", mapHandler(queue));
        session.send("/app/send", Map.of("sender", "Alice", "text", "ts check"));

        Map<String, Object> received = queue.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat((String) received.get("timestamp")).matches("\\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    @Order(4)
    @DisplayName("STOMP: сервер добавляет timestamp, даже если клиент не прислал поле")
    void serverAddsTimestamp() throws Exception {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingDeque<>();

        StompSession session = connect();
        session.subscribe("/topic/messages", mapHandler(queue));
        // клиент отправляет только sender и text — без timestamp
        session.send("/app/send", Map.of("sender", "Alice", "text", "no ts"));

        Map<String, Object> received = queue.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.get("timestamp")).isNotNull();
        assertThat((String) received.get("timestamp")).matches("\\d{2}:\\d{2}:\\d{2}");
    }

    // -- broadcast -------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("STOMP: два клиента получают одно сообщение одновременно")
    void broadcastToMultipleClients() throws Exception {
        BlockingQueue<Map<String, Object>> queue1 = new LinkedBlockingDeque<>();
        BlockingQueue<Map<String, Object>> queue2 = new LinkedBlockingDeque<>();

        StompSession s1 = connect();
        s1.subscribe("/topic/messages", mapHandler(queue1));

        StompSession s2 = connect();
        s2.subscribe("/topic/messages", mapHandler(queue2));

        s1.send("/app/send", Map.of("sender", "Alice", "text", "broadcast"));

        Map<String, Object> msg1 = queue1.poll(5, TimeUnit.SECONDS);
        Map<String, Object> msg2 = queue2.poll(5, TimeUnit.SECONDS);

        assertThat(msg1).isNotNull();
        assertThat(msg2).isNotNull();
        assertThat(msg1.get("text")).isEqualTo("broadcast");
        assertThat(msg2.get("text")).isEqualTo("broadcast");
    }

    @Test
    @Order(6)
    @DisplayName("STOMP: sender и text передаются без искажений")
    void fieldsPreservedExactly() throws Exception {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingDeque<>();

        StompSession session = connect();
        session.subscribe("/topic/messages", mapHandler(queue));
        session.send("/app/send", Map.of("sender", "Кирилл", "text", "Привет мир!"));

        Map<String, Object> received = queue.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.get("sender")).isEqualTo("Кирилл");
        assertThat(received.get("text")).isEqualTo("Привет мир!");
    }

    // -- helpers ---------------------------------------------------------------

    private StompSession connect() throws Exception {
        return stompClient
                .connectAsync("ws://localhost:{port}/ws", new StompSessionHandlerAdapter() {}, port)
                .get(5, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private StompFrameHandler mapHandler(BlockingQueue<Map<String, Object>> queue) {
        return new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return Map.class; }
            @Override public void handleFrame(StompHeaders h, Object p) { queue.offer((Map<String, Object>) p); }
        };
    }
}
