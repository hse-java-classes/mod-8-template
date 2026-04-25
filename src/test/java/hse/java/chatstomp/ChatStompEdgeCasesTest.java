package hse.java.chatstomp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hse.java.practice.chatstomp.ChatStompApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Edge-кейсы и потокобезопасность. Тег: chat-stomp-3 (2 балла).
 *
 * Множественная отправка, параллельные клиенты, порядок в /history, подписка после отправки.
 */
@Tag("chat-stomp-3")
@SpringBootTest(
    classes = ChatStompApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatStompEdgeCasesTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    private WebSocketStompClient stompClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    // -- concurrent writes -----------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Параллельная отправка: все сообщения попадают в /history")
    void concurrentSendsAllPersisted() throws Exception {
        int count = 10;
        ExecutorService pool = Executors.newFixedThreadPool(count);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int idx = i;
            futures.add(pool.submit(() -> {
                try {
                    sendViaStomp("user-" + idx, "msg-" + idx);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        pool.shutdown();

        String body = mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode array = objectMapper.readTree(body);
        assertThat(array.size()).isEqualTo(count);
    }

    // -- multiple messages from one client -------------------------------------

    @Test
    @Order(2)
    @DisplayName("Один клиент отправляет несколько сообщений подряд — все доходят")
    void multipleMessagesFromOneClient() throws Exception {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingDeque<>();

        StompSession session = connect();
        session.subscribe("/topic/messages", mapHandler(queue));

        session.send("/app/send", Map.of("sender", "A", "text", "m1"));
        session.send("/app/send", Map.of("sender", "A", "text", "m2"));
        session.send("/app/send", Map.of("sender", "A", "text", "m3"));

        List<String> received = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> msg = queue.poll(5, TimeUnit.SECONDS);
            assertThat(msg).isNotNull();
            received.add((String) msg.get("text"));
        }

        assertThat(received).containsExactly("m1", "m2", "m3");
    }

    // -- subscriber after send -------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("Новый подписчик не получает старые STOMP-сообщения (история — через REST)")
    void lateSubscriberDoesNotGetOldMessages() throws Exception {
        sendViaStomp("A", "before subscribe");

        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingDeque<>();
        StompSession session = connect();
        session.subscribe("/topic/messages", mapHandler(queue));

        // новый подписчик не должен получить старое сообщение через STOMP
        Map<String, Object> ghost = queue.poll(2, TimeUnit.SECONDS);
        assertThat(ghost).isNull();

        // но через REST история доступна
        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("before subscribe"));
    }

    // -- three clients interact ------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("Три клиента: каждый видит сообщения остальных")
    void threeClientsInteract() throws Exception {
        BlockingQueue<Map<String, Object>> q1 = new LinkedBlockingDeque<>();
        BlockingQueue<Map<String, Object>> q2 = new LinkedBlockingDeque<>();
        BlockingQueue<Map<String, Object>> q3 = new LinkedBlockingDeque<>();

        StompSession s1 = connect();
        s1.subscribe("/topic/messages", mapHandler(q1));
        StompSession s2 = connect();
        s2.subscribe("/topic/messages", mapHandler(q2));
        StompSession s3 = connect();
        s3.subscribe("/topic/messages", mapHandler(q3));

        s1.send("/app/send", Map.of("sender", "A", "text", "from A"));
        s2.send("/app/send", Map.of("sender", "B", "text", "from B"));
        s3.send("/app/send", Map.of("sender", "C", "text", "from C"));

        // каждый клиент должен получить все 3 сообщения
        for (BlockingQueue<Map<String, Object>> q : List.of(q1, q2, q3)) {
            List<String> texts = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Map<String, Object> msg = q.poll(5, TimeUnit.SECONDS);
                assertThat(msg).as("Ожидалось 3 сообщения в каждой очереди").isNotNull();
                texts.add((String) msg.get("text"));
            }
            assertThat(texts).containsExactlyInAnyOrder("from A", "from B", "from C");
        }
    }

    // -- history reflects STOMP order ------------------------------------------

    @Test
    @Order(5)
    @DisplayName("/history сохраняет порядок отправки STOMP-сообщений")
    void historyReflectsStompOrder() throws Exception {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingDeque<>();
        StompSession session = connect();
        session.subscribe("/topic/messages", mapHandler(queue));

        for (int i = 1; i <= 5; i++) {
            session.send("/app/send", Map.of("sender", "S", "text", "m" + i));
            // ждём подтверждения доставки перед следующей отправкой
            assertThat(queue.poll(5, TimeUnit.SECONDS)).isNotNull();
        }

        String body = mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode array = objectMapper.readTree(body);
        assertThat(array.size()).isEqualTo(5);
        for (int i = 0; i < 5; i++) {
            assertThat(array.get(i).path("text").asText()).isEqualTo("m" + (i + 1));
        }
    }

    // -- helpers ---------------------------------------------------------------

    private StompSession connect() throws Exception {
        return stompClient
                .connectAsync("ws://localhost:{port}/ws", new StompSessionHandlerAdapter() {}, port)
                .get(5, TimeUnit.SECONDS);
    }

    private void sendViaStomp(String sender, String text) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(
            new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompSession session = client
                .connectAsync("ws://localhost:{port}/ws", new StompSessionHandlerAdapter() {}, port)
                .get(5, TimeUnit.SECONDS);
        session.send("/app/send", Map.of("sender", sender, "text", text));
        Thread.sleep(500);
        session.disconnect();
    }

    @SuppressWarnings("unchecked")
    private StompFrameHandler mapHandler(BlockingQueue<Map<String, Object>> queue) {
        return new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return Map.class; }
            @Override public void handleFrame(StompHeaders h, Object p) { queue.offer((Map<String, Object>) p); }
        };
    }
}
