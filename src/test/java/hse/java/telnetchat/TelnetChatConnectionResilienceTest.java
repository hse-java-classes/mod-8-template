package hse.java.telnetchat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("telnet-chat-7")
class TelnetChatConnectionResilienceTest {

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
    @DisplayName("Разрывы до ника и внутри комнаты не роняют сервер")
    void serverSurvivesEarlyAndAbruptDisconnects() throws Exception {
        Socket ghost = ServerDriver.connect(port);
        ServerDriver.readAvailable(ServerDriver.reader(ghost));
        ghost.close();

        Socket stable = ServerDriver.connect(port);
        BufferedReader inStable = ServerDriver.reader(stable);
        PrintWriter outStable = ServerDriver.writer(stable);
        ServerDriver.readAvailable(inStable);
        ServerDriver.send(outStable, "Stable");
        ServerDriver.readAvailable(inStable);
        ServerDriver.send(outStable, "1");
        ServerDriver.readAvailable(inStable);
        ServerDriver.send(outStable, "res-room");
        ServerDriver.readAvailable(inStable);
        ServerDriver.send(outStable, "3");
        ServerDriver.readAvailable(inStable);
        ServerDriver.send(outStable, "res-room");
        ServerDriver.readAvailable(inStable);

        Socket broken = ServerDriver.connect(port);
        BufferedReader inBroken = ServerDriver.reader(broken);
        PrintWriter outBroken = ServerDriver.writer(broken);
        ServerDriver.readAvailable(inBroken);
        ServerDriver.send(outBroken, "Broken");
        ServerDriver.readAvailable(inBroken);
        ServerDriver.send(outBroken, "3");
        ServerDriver.readAvailable(inBroken);
        ServerDriver.send(outBroken, "res-room");
        ServerDriver.readAvailable(inBroken);
        ServerDriver.readAvailable(inStable);
        broken.close();

        try (Socket probe = ServerDriver.connect(port)) {
            BufferedReader inProbe = ServerDriver.reader(probe);
            PrintWriter outProbe = ServerDriver.writer(probe);
            List<String> prompt = ServerDriver.readAvailable(inProbe);
            assertFalse(prompt.isEmpty(), "Сервер должен принимать новые подключения после разрывов.");

            ServerDriver.send(outProbe, "Probe");
            List<String> afterNick = ServerDriver.readAvailable(inProbe);
            String joined = String.join(" ", afterNick).toLowerCase();
            assertTrue(joined.contains("меню") || joined.contains("1") || joined.contains("create"));
        }

        stable.close();
    }
}
