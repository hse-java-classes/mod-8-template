package hse.java.echochat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Базовые тесты эхо-чата. Тег: echo-chat-1 (2 балла из 6).
 *
 * Проверяют базовый сценарий:
 * 1. Сервер запрашивает никнейм при подключении.
 * 2. Сервер подтверждает вход после ввода никнейма.
 * 3. Сообщение рассылается всем участникам с никнеймом отправителя.
 *
 * Запустите сервер: hse.java.practice.echochat.EchoChatServer <port>
 */
@Tag("echo-chat-1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EchoChatHappyPathTest {

    private static final String SERVER_CLASS = "hse.java.practice.echochat.EchoChatServer";
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        port = ServerDriver.freePort();
        ServerDriver.startServer(SERVER_CLASS, port);
        ServerDriver.waitReady(port);
    }

    // -------------------------------------------------------------------------
    // Тест 1: сервер принимает подключение и запрашивает никнейм
    // -------------------------------------------------------------------------
    @Test
    @Order(1)
    @Timeout(5)
    @DisplayName("Сервер запрашивает никнейм при подключении")
    void serverAsksForNickname() throws Exception {
        try (Socket client = ServerDriver.connect(port)) {
            BufferedReader in = ServerDriver.reader(client);
            List<String> greeting = ServerDriver.readAvailable(in);

            assertFalse(greeting.isEmpty(),
                    "Сервер должен что-то написать при подключении");

            String joined = String.join(" ", greeting).toLowerCase();
            assertTrue(joined.contains("nick") || joined.contains("никнейм") || joined.contains("имя") || joined.contains("логин"),
                    "Сервер должен запросить никнейм. Получено: " + greeting);
        }
    }

    // -------------------------------------------------------------------------
    // Тест 2: клиент вводит никнейм — сервер подтверждает вход
    // -------------------------------------------------------------------------
    @Test
    @Order(2)
    @Timeout(5)
    @DisplayName("После ввода никнейма сервер подтверждает вход")
    void serverConfirmsNickname() throws Exception {
        try (Socket client = ServerDriver.connect(port)) {
            BufferedReader in = ServerDriver.reader(client);
            PrintWriter out = ServerDriver.writer(client);

            ServerDriver.readAvailable(in); // пропускаем приглашение
            ServerDriver.send(out, "TestUser");

            List<String> response = ServerDriver.readAvailable(in);
            String joined = String.join(" ", response).toLowerCase();

            assertTrue(joined.contains("testuser") || joined.contains("вошёл") || joined.contains("welcome"),
                    "Сервер должен подтвердить вход или показать никнейм. Получено: " + response);
        }
    }

    // -------------------------------------------------------------------------
    // Тест 3: сообщение от одного клиента доходит до другого с никнеймом
    // -------------------------------------------------------------------------
    @Test
    @Order(3)
    @Timeout(5)
    @DisplayName("Сообщение рассылается всем с никнеймом отправителя")
    void messageBroadcastedWithNickname() throws Exception {
        try (Socket alice = ServerDriver.connect(port);
             Socket bob   = ServerDriver.connect(port)) {

            BufferedReader inAlice = ServerDriver.reader(alice);
            BufferedReader inBob   = ServerDriver.reader(bob);
            PrintWriter    outAlice = ServerDriver.writer(alice);
            PrintWriter    outBob   = ServerDriver.writer(bob);

            // оба вводят никнеймы
            ServerDriver.readAvailable(inAlice);
            ServerDriver.send(outAlice, "Alice");
            ServerDriver.readAvailable(inAlice); // подтверждение

            ServerDriver.readAvailable(inBob);
            ServerDriver.send(outBob, "Bob");
            ServerDriver.readAvailable(inBob);   // подтверждение + уведомление о Bob

            // Alice пишет сообщение
            ServerDriver.send(outAlice, "Hello from Alice");

            // Bob должен получить сообщение с никнеймом Alice
            List<String> bobReceived = ServerDriver.readAvailable(inBob);
            String joined = String.join(" ", bobReceived);

            assertTrue(joined.contains("Hello from Alice"),
                    "Bob должен получить текст сообщения. Получено: " + bobReceived);
            assertTrue(joined.contains("Alice"),
                    "Bob должен видеть никнейм отправителя. Получено: " + bobReceived);
        }
    }
}
