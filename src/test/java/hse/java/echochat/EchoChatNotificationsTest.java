package hse.java.echochat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты уведомлений о входе и выходе. Тег: echo-chat-3 (2 балла из 6).
 */
@Tag("echo-chat-3")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EchoChatNotificationsTest {

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
    @DisplayName("Все участники получают уведомление о входе нового пользователя")
    void joinNotificationSentToAll() throws Exception {
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

        Thread.sleep(200);

        List<String> aliceMsg = ServerDriver.readAvailable(inAlice);
        String text = String.join(" ", aliceMsg).toLowerCase();

        assertTrue(text.contains("bob"),
                "Alice должна получить уведомление с ником Bob. Получено: " + aliceMsg);
        assertTrue(text.contains("вошёл") || text.contains("вошла") || text.contains("подключ") || text.contains("joined") || text.contains("enter"),
                "Alice должна получить уведомление о входе. Получено: " + aliceMsg);

        alice.close();
        bob.close();
    }

    @Test
    @Order(2)
    @Timeout(5)
    @DisplayName("Все участники получают уведомление о выходе пользователя")
    void leaveNotificationSentToAll() throws Exception {
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
        String text = String.join(" ", aliceMsg).toLowerCase();

        assertTrue(text.contains("bob"),
                "Alice должна получить уведомление с ником Bob. Получено: " + aliceMsg);
        assertTrue(text.contains("покин") || text.contains("вышел") || text.contains("вышла") || text.contains("отключ") || text.contains("left"),
                "Alice должна получить уведомление о выходе Bob. Получено: " + aliceMsg);

        alice.close();
    }

    @Test
    @Order(3)
    @Timeout(6)
    @DisplayName("Три участника — уведомления о входе и выходе доходят до всех")
    void threeClientsAllNotified() throws Exception {
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

        Socket carol = ServerDriver.connect(port);
        BufferedReader inCarol = ServerDriver.reader(carol);
        PrintWriter outCarol = ServerDriver.writer(carol);

        ServerDriver.readAvailable(inCarol);
        ServerDriver.send(outCarol, "Carol");
        ServerDriver.readAvailable(inCarol);

        Thread.sleep(300);

        List<String> aliceJoin = ServerDriver.readAvailable(inAlice);
        List<String> bobJoin = ServerDriver.readAvailable(inBob);

        assertTrue(String.join(" ", aliceJoin).contains("Carol"),
                "Alice должна получить уведомление о входе Carol. Получено: " + aliceJoin);
        assertTrue(String.join(" ", bobJoin).contains("Carol"),
                "Bob должен получить уведомление о входе Carol. Получено: " + bobJoin);

        carol.close();
        Thread.sleep(400);

        List<String> aliceLeave = ServerDriver.readAvailable(inAlice);
        List<String> bobLeave = ServerDriver.readAvailable(inBob);

        assertTrue(String.join(" ", aliceLeave).contains("Carol"),
                "Alice должна получить уведомление о выходе Carol. Получено: " + aliceLeave);
        assertTrue(String.join(" ", bobLeave).contains("Carol"),
                "Bob должен получить уведомление о выходе Carol. Получено: " + bobLeave);

        alice.close();
        bob.close();
    }
}
