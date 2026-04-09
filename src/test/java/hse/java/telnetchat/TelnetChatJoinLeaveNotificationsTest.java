package hse.java.telnetchat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("telnet-chat-5")
class TelnetChatJoinLeaveNotificationsTest {

    private static final String SERVER_CLASS = "hse.java.homework.telnetchat.TelnetChatServer";
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        port = ServerDriver.freePort();
        ServerDriver.startServer(SERVER_CLASS, port);
        ServerDriver.waitReady(port);
    }

    @Test
    @Timeout(7)
    @DisplayName("Участники получают уведомления о входе и выходе из комнаты")
    void roomJoinAndExitAreNotified() throws Exception {
        try (Socket alice = ServerDriver.connect(port);
             Socket bob = ServerDriver.connect(port)) {
            BufferedReader inA = ServerDriver.reader(alice);
            BufferedReader inB = ServerDriver.reader(bob);
            PrintWriter outA = ServerDriver.writer(alice);
            PrintWriter outB = ServerDriver.writer(bob);

            ServerDriver.readAvailable(inA);
            ServerDriver.send(outA, "Alice");
            ServerDriver.readAvailable(inA);

            ServerDriver.send(outA, "1");
            ServerDriver.readAvailable(inA);
            ServerDriver.send(outA, "notify-room");
            ServerDriver.readAvailable(inA);
            ServerDriver.send(outA, "3");
            ServerDriver.readAvailable(inA);
            ServerDriver.send(outA, "notify-room");
            ServerDriver.readAvailable(inA);

            ServerDriver.readAvailable(inB);
            ServerDriver.send(outB, "Bob");
            ServerDriver.readAvailable(inB);
            ServerDriver.send(outB, "3");
            ServerDriver.readAvailable(inB);
            ServerDriver.send(outB, "notify-room");
            ServerDriver.readAvailable(inB);

            List<String> aliceJoinNotice = ServerDriver.readAvailable(inA);
            String joinText = String.join(" ", aliceJoinNotice).toLowerCase();
            assertTrue(joinText.contains("bob"));
            assertTrue(joinText.contains("вош") || joinText.contains("join"));

            ServerDriver.send(outB, "/exit");
            List<String> aliceLeaveNotice = ServerDriver.readAvailable(inA);
            String leaveText = String.join(" ", aliceLeaveNotice).toLowerCase();
            assertTrue(leaveText.contains("bob"));
            assertTrue(leaveText.contains("покин") || leaveText.contains("выш") || leaveText.contains("left"));
        }
    }
}
