package hse.java.telnetchat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("telnet-chat-4")
class TelnetChatRoomMessagingIsolationTest {

    private static final String SERVER_CLASS = "hse.java.homework.telnetchat.TelnetChatServer";
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        port = ServerDriver.freePort();
        ServerDriver.startServer(SERVER_CLASS, port);
        ServerDriver.waitReady(port);
    }

    @Test
    @Timeout(8)
    @DisplayName("Сообщения видят только участники своей комнаты")
    void messagesDoNotLeakAcrossRooms() throws Exception {
        try (Socket alice = ServerDriver.connect(port);
             Socket bob = ServerDriver.connect(port);
             Socket carol = ServerDriver.connect(port)) {

            BufferedReader inA = ServerDriver.reader(alice);
            BufferedReader inB = ServerDriver.reader(bob);
            BufferedReader inC = ServerDriver.reader(carol);
            PrintWriter outA = ServerDriver.writer(alice);
            PrintWriter outB = ServerDriver.writer(bob);
            PrintWriter outC = ServerDriver.writer(carol);

            ServerDriver.readAvailable(inA);
            ServerDriver.send(outA, "Alice");
            ServerDriver.readAvailable(inA);
            ServerDriver.readAvailable(inB);
            ServerDriver.send(outB, "Bob");
            ServerDriver.readAvailable(inB);
            ServerDriver.readAvailable(inC);
            ServerDriver.send(outC, "Carol");
            ServerDriver.readAvailable(inC);

            ServerDriver.send(outA, "1");
            ServerDriver.readAvailable(inA);
            ServerDriver.send(outA, "room-a");
            ServerDriver.readAvailable(inA);
            ServerDriver.send(outA, "3");
            ServerDriver.readAvailable(inA);
            ServerDriver.send(outA, "room-a");
            ServerDriver.readAvailable(inA);

            ServerDriver.send(outB, "1");
            ServerDriver.readAvailable(inB);
            ServerDriver.send(outB, "room-b");
            ServerDriver.readAvailable(inB);
            ServerDriver.send(outB, "3");
            ServerDriver.readAvailable(inB);
            ServerDriver.send(outB, "room-b");
            ServerDriver.readAvailable(inB);

            ServerDriver.send(outC, "3");
            ServerDriver.readAvailable(inC);
            ServerDriver.send(outC, "room-a");
            ServerDriver.readAvailable(inC);
            ServerDriver.readAvailable(inA);

            ServerDriver.send(outA, "hello-room-a");
            List<String> toCarol = ServerDriver.readAvailable(inC);
            List<String> toBob = ServerDriver.readAvailable(inB);
            List<String> toAlice = ServerDriver.readAvailable(inA);

            String carolText = String.join(" ", toCarol);
            String bobText = String.join(" ", toBob);
            String aliceText = String.join(" ", toAlice);

            assertTrue(carolText.contains("hello-room-a"));
            assertFalse(bobText.contains("hello-room-a"));
            assertFalse(aliceText.contains("hello-room-a"));
        }
    }
}
