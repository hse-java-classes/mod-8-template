package hse.java.telnetchat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("telnet-chat-2")
class TelnetChatNicknameValidationTest {

    private static final String SERVER_CLASS = "hse.java.homework.telnetchat.TelnetChatServer";
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        port = ServerDriver.freePort();
        ServerDriver.startServer(SERVER_CLASS, port);
        ServerDriver.waitReady(port);
    }

    @Test
    @Timeout(5)
    @DisplayName("Пустой ник не принимается, повторный ввод работает")
    void emptyNicknameRejected() throws Exception {
        try (Socket client = ServerDriver.connect(port)) {
            BufferedReader in = ServerDriver.reader(client);
            PrintWriter out = ServerDriver.writer(client);

            List<String> prompt = ServerDriver.readAvailable(in);
            assertFalse(prompt.isEmpty(), "Сервер должен запросить ник.");

            ServerDriver.send(out, "");
            List<String> afterEmpty = ServerDriver.readAvailable(in);
            String joined = String.join(" ", afterEmpty).toLowerCase();
            assertTrue(joined.contains("ник") || joined.contains("nick") || joined.contains("пуст"));

            ServerDriver.send(out, "Alice");
            List<String> afterValid = ServerDriver.readAvailable(in);
            String menu = String.join(" ", afterValid).toLowerCase();
            assertTrue(menu.contains("1") || menu.contains("меню") || menu.contains("create"));
        }
    }
}
