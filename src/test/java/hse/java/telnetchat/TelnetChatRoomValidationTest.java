package hse.java.telnetchat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("telnet-chat-3")
class TelnetChatRoomValidationTest {

    private static final String SERVER_CLASS = "hse.java.homework.telnetchat.TelnetChatServer";
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        port = ServerDriver.freePort();
        ServerDriver.startServer(SERVER_CLASS, port);
        ServerDriver.waitReady(port);
    }

    @Test
    @Timeout(6)
    @DisplayName("Пустое и дублирующееся имя комнаты не принимаются")
    void emptyAndDuplicateRoomNameRejected() throws Exception {
        try (Socket client = ServerDriver.connect(port)) {
            BufferedReader in = ServerDriver.reader(client);
            PrintWriter out = ServerDriver.writer(client);

            ServerDriver.readAvailable(in);
            ServerDriver.send(out, "Maker");
            ServerDriver.readAvailable(in);

            ServerDriver.send(out, "1");
            ServerDriver.readAvailable(in);
            ServerDriver.send(out, "");
            List<String> afterEmpty = ServerDriver.readAvailable(in);
            String emptyText = String.join(" ", afterEmpty).toLowerCase();
            assertTrue(emptyText.contains("пуст") || emptyText.contains("ошиб") || emptyText.contains("invalid"));

            ServerDriver.send(out, "1");
            ServerDriver.readAvailable(in);
            ServerDriver.send(out, "dup-room");
            ServerDriver.readAvailable(in);

            ServerDriver.send(out, "1");
            ServerDriver.readAvailable(in);
            ServerDriver.send(out, "dup-room");
            List<String> afterDup = ServerDriver.readAvailable(in);
            String dupText = String.join(" ", afterDup).toLowerCase();
            assertTrue(dupText.contains("уже") || dupText.contains("exists") || dupText.contains("ошиб"));
        }
    }
}
