package hse.java.telnetchat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("telnet-chat-6")
class TelnetChatDeleteRoomPermissionsTest {

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
    @DisplayName("/delete доступен только владельцу, удаление возвращает участников в меню")
    void deleteRoomRequiresOwnership() throws Exception {
        try (Socket owner = ServerDriver.connect(port);
             Socket guest = ServerDriver.connect(port)) {
            BufferedReader inOwner = ServerDriver.reader(owner);
            BufferedReader inGuest = ServerDriver.reader(guest);
            PrintWriter outOwner = ServerDriver.writer(owner);
            PrintWriter outGuest = ServerDriver.writer(guest);

            ServerDriver.readAvailable(inOwner);
            ServerDriver.send(outOwner, "Owner");
            ServerDriver.readAvailable(inOwner);
            ServerDriver.send(outOwner, "1");
            ServerDriver.readAvailable(inOwner);
            ServerDriver.send(outOwner, "delete-room");
            ServerDriver.readAvailable(inOwner);
            ServerDriver.send(outOwner, "3");
            ServerDriver.readAvailable(inOwner);
            ServerDriver.send(outOwner, "delete-room");
            ServerDriver.readAvailable(inOwner);

            ServerDriver.readAvailable(inGuest);
            ServerDriver.send(outGuest, "Guest");
            ServerDriver.readAvailable(inGuest);
            ServerDriver.send(outGuest, "3");
            ServerDriver.readAvailable(inGuest);
            ServerDriver.send(outGuest, "delete-room");
            ServerDriver.readAvailable(inGuest);
            ServerDriver.readAvailable(inOwner);

            ServerDriver.send(outGuest, "/delete");
            List<String> guestDenied = ServerDriver.readAvailable(inGuest);
            String deniedText = String.join(" ", guestDenied).toLowerCase();
            assertTrue(deniedText.contains("только") || deniedText.contains("созд") || deniedText.contains("owner"));

            ServerDriver.send(outOwner, "/delete");
            List<String> ownerAfterDelete = ServerDriver.readAvailable(inOwner);
            List<String> guestAfterDelete = ServerDriver.readAvailable(inGuest);

            String ownerText = String.join(" ", ownerAfterDelete).toLowerCase();
            String guestText = String.join(" ", guestAfterDelete).toLowerCase();

            assertTrue(ownerText.contains("удален") || ownerText.contains("удалена") || ownerText.contains("deleted"));
            assertTrue(guestText.contains("удален") || guestText.contains("удалена") || guestText.contains("deleted"));
            assertTrue(ownerText.contains("меню") || ownerText.contains("1"));
            assertTrue(guestText.contains("меню") || guestText.contains("1"));
        }
    }
}
