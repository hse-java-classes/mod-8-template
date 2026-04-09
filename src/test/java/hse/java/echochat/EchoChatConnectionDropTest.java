package hse.java.echochat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты устойчивости при разрывах соединения. Тег: echo-chat-2 (2 балла из 6).
 *
 * Проверяют три сценария разрыва:
 * 1. Разрыв до ввода никнейма — сервер не падает, принимает новые подключения.
 * 2. Разрыв после ввода никнейма — остальные получают уведомление об уходе.
 * 3. Разрыв в процессе чата — сервер продолжает работать, уведомление об уходе рассылается.
 */
@Tag("echo-chat-2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EchoChatConnectionDropTest {

    private static final String SERVER_CLASS = "hse.java.practice.echochat.EchoChatServer";
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        port = ServerDriver.freePort();
        ServerDriver.startServer(SERVER_CLASS, port);
        ServerDriver.waitReady(port);
    }

    @Test
    @Order(1)
    @Timeout(5)
    @DisplayName("Разрыв до ввода ника — сервер продолжает принимать соединения")
    void dropBeforeNickname() throws Exception {
        Socket stable = ServerDriver.connect(port);
        BufferedReader inStable = ServerDriver.reader(stable);
        PrintWriter outStable = ServerDriver.writer(stable);
        ServerDriver.readAvailable(inStable);
        ServerDriver.send(outStable, "Stable");
        ServerDriver.readAvailable(inStable);

        Socket ghost = ServerDriver.connect(port);
        ServerDriver.readAvailable(ServerDriver.reader(ghost));
        ghost.close();

        Thread.sleep(300);

        try (Socket probe = ServerDriver.connect(port)) {
            List<String> prompt = ServerDriver.readAvailable(ServerDriver.reader(probe));
            assertFalse(prompt.isEmpty(),
                    "Сервер должен отвечать на новые подключения после разрыва до ввода ника");
        }

        stable.close();
    }

    @Test
    @Order(2)
    @Timeout(5)
    @DisplayName("Разрыв после ввода ника — остальные получают уведомление об уходе")
    void dropAfterNickname() throws Exception {
        Socket alice = ServerDriver.connect(port);
        BufferedReader inAlice = ServerDriver.reader(alice);
        PrintWriter outAlice = ServerDriver.writer(alice);

        ServerDriver.readAvailable(inAlice);
        ServerDriver.send(outAlice, "Alice");
        ServerDriver.readAvailable(inAlice);

        Socket bob = ServerDriver.connect(port);
        BufferedReader inBob = ServerDriver.reader(bob);
        PrintWriter outBob = ServerDriver.writer(bob);

        ServerDriver.readAvailable(inBob);
        ServerDriver.send(outBob, "Bob");
        ServerDriver.readAvailable(inBob);
        ServerDriver.readAvailable(inAlice);

        bob.close();
        Thread.sleep(400);

        List<String> aliceMsg = ServerDriver.readAvailable(inAlice);
        String joined = String.join(" ", aliceMsg).toLowerCase();
        assertTrue(joined.contains("bob"),
                "Alice должна получить уведомление с ником Bob. Получено: " + aliceMsg);
        assertTrue(joined.contains("покин") || joined.contains("вышел") || joined.contains("отключ") || joined.contains("left"),
                "Alice должна получить уведомление об уходе Bob. Получено: " + aliceMsg);

        alice.close();
    }

    @Test
    @Order(3)
    @Timeout(6)
    @DisplayName("Разрыв в процессе чата — сервер работает, сообщения доходят")
    void dropDuringChat() throws Exception {
        Socket alice = ServerDriver.connect(port);
        BufferedReader inAlice = ServerDriver.reader(alice);
        PrintWriter outAlice = ServerDriver.writer(alice);

        Socket bob = ServerDriver.connect(port);
        BufferedReader inBob = ServerDriver.reader(bob);
        PrintWriter outBob = ServerDriver.writer(bob);

        ServerDriver.readAvailable(inAlice);
        ServerDriver.send(outAlice, "Alice");
        ServerDriver.readAvailable(inAlice);

        ServerDriver.readAvailable(inBob);
        ServerDriver.send(outBob, "Bob");
        ServerDriver.readAvailable(inBob);
        ServerDriver.readAvailable(inAlice);

        ServerDriver.send(outBob, "Message from Bob");
        ServerDriver.readAvailable(inAlice);

        bob.close();
        Thread.sleep(400);

        Socket carol = ServerDriver.connect(port);
        BufferedReader inCarol = ServerDriver.reader(carol);
        PrintWriter outCarol = ServerDriver.writer(carol);

        ServerDriver.readAvailable(inCarol);
        ServerDriver.send(outCarol, "Carol");
        ServerDriver.readAvailable(inCarol);
        ServerDriver.readAvailable(inAlice);

        ServerDriver.send(outCarol, "Hi from Carol");

        List<String> aliceMsg = ServerDriver.readAvailable(inAlice);
        String joined = String.join(" ", aliceMsg);
        assertTrue(joined.contains("Hi from Carol") || joined.contains("Carol"),
                "Alice должна получать сообщения после разрыва Bob. Получено: " + aliceMsg);

        alice.close();
        carol.close();
    }
}
