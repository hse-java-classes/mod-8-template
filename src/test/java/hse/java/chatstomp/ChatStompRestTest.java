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
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST-эндпоинты чата. Тег: chat-stomp-1 (2 балла).
 *
 * Проверяет GET /history и GET /.
 */
@Tag("chat-stomp-1")
@SpringBootTest(
    classes = ChatStompApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatStompRestTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -- GET /history ----------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("GET /history — 200 и пустой массив при старте")
    void historyIsEmptyOnStart() throws Exception {
        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(2)
    @DisplayName("GET /history — Content-Type содержит application/json")
    void historyContentType() throws Exception {
        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /history — после отправки содержит sender, text, timestamp")
    void historyJsonStructure() throws Exception {
        sendViaStomp("Bob", "check");

        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sender").value("Bob"))
                .andExpect(jsonPath("$[0].text").value("check"))
                .andExpect(jsonPath("$[0].timestamp").exists());
    }

    @Test
    @Order(4)
    @DisplayName("GET /history — timestamp имеет формат HH:mm:ss")
    void historyTimestampFormat() throws Exception {
        sendViaStomp("Alice", "Hello history");

        String body = mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode array = objectMapper.readTree(body);
        for (JsonNode msg : array) {
            String ts = msg.path("timestamp").asText();
            Assertions.assertTrue(ts.matches("\\d{2}:\\d{2}:\\d{2}"),
                    "timestamp должен быть HH:mm:ss, получено: " + ts);
        }
    }

    @Test
    @Order(5)
    @DisplayName("GET /history — несколько сообщений сохраняются по порядку")
    void historyPreservesOrder() throws Exception {
        sendViaStomp("A", "first");
        sendViaStomp("B", "second");
        sendViaStomp("C", "third");

        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].text").value("first"))
                .andExpect(jsonPath("$[1].text").value("second"))
                .andExpect(jsonPath("$[2].text").value("third"));
    }

    // -- GET / -----------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("GET / — 200 и HTML-страница")
    void indexPageReturnsHtml() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @Order(7)
    @DisplayName("GET / — HTML содержит подключение к SockJS/STOMP")
    void indexPageContainsStomp() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("stomp")));
    }

    // -- helpers ---------------------------------------------------------------

    private void sendViaStomp(String sender, String text) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(
            new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        StompSession session = stompClient
                .connectAsync("ws://localhost:{port}/ws", new StompSessionHandlerAdapter() {}, port)
                .get(5, TimeUnit.SECONDS);
        session.send("/app/send", Map.of("sender", sender, "text", text));
        Thread.sleep(500);
        session.disconnect();
    }
}
