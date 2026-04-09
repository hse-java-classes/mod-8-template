package hse.java.telnetchat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Happy-path тесты для Telnet-чата с комнатами. Тег: telnet-chat-1 (1 балл из 7).
 *
 * Запустите сервер: hse.java.homework.telnetchat.TelnetChatServer <port>
 */
@Tag("telnet-chat-1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TelnetChatHappyPathTest {

    private static final String SERVER_CLASS = "hse.java.homework.telnetchat.TelnetChatServer";
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        port = ServerDriver.freePort();
        ServerDriver.startServer(SERVER_CLASS, port);
        ServerDriver.waitReady(port);
    }

    // -------------------------------------------------------------------------
    // Тест 1: сервер запрашивает никнейм, после ввода показывает меню
    // -------------------------------------------------------------------------
    @Test
    @Order(1)
    @Timeout(5)
    @DisplayName("После ввода никнейма показывается главное меню")
    void menuShownAfterNickname() throws Exception {
        try (Socket client = ServerDriver.connect(port)) {
            BufferedReader in  = ServerDriver.reader(client);
            PrintWriter    out = ServerDriver.writer(client);

            List<String> prompt = ServerDriver.readAvailable(in);
            assertFalse(prompt.isEmpty(), "Сервер должен запросить никнейм. Ничего не получено.");

            ServerDriver.send(out, "Alice");

            List<String> menu = ServerDriver.readAvailable(in);
            String joined = String.join(" ", menu).toLowerCase();

            // меню должно содержать варианты действий
            assertTrue(joined.contains("1") || joined.contains("создат") || joined.contains("creat"),
                    "Меню должно содержать пункт создания комнаты. Получено: " + menu);
            assertTrue(joined.contains("2") || joined.contains("список") || joined.contains("list"),
                    "Меню должно содержать пункт списка комнат. Получено: " + menu);
            assertTrue(joined.contains("3") || joined.contains("подключ") || joined.contains("join"),
                    "Меню должно содержать пункт подключения к комнате. Получено: " + menu);
        }
    }

    // -------------------------------------------------------------------------
    // Тест 2: создание комнаты — сервер подтверждает создание
    // -------------------------------------------------------------------------
    @Test
    @Order(2)
    @Timeout(5)
    @DisplayName("Пользователь может создать комнату")
    void canCreateRoom() throws Exception {
        try (Socket client = ServerDriver.connect(port)) {
            BufferedReader in  = ServerDriver.reader(client);
            PrintWriter    out = ServerDriver.writer(client);

            // вход
            ServerDriver.readAvailable(in);
            ServerDriver.send(out, "Bob");
            ServerDriver.readAvailable(in); // меню

            // выбрать "создать комнату"
            ServerDriver.send(out, "1");
            ServerDriver.readAvailable(in);

            // ввести имя комнаты
            ServerDriver.send(out, "happy-room");
            List<String> result = ServerDriver.readAvailable(in);

            String joined = String.join(" ", result).toLowerCase();
            assertTrue(
                joined.contains("happy-room") || joined.contains("создан") || joined.contains("created"),
                "Сервер должен подтвердить создание комнаты. Получено: " + result
            );
        }
    }

    // -------------------------------------------------------------------------
    // Тест 3: список комнат показывает созданную комнату
    // -------------------------------------------------------------------------
    @Test
    @Order(3)
    @Timeout(5)
    @DisplayName("Список комнат содержит созданную комнату")
    void roomAppearsInList() throws Exception {
        // Клиент 1: создаёт комнату
        try (Socket creator = ServerDriver.connect(port)) {
            BufferedReader inC  = ServerDriver.reader(creator);
            PrintWriter    outC = ServerDriver.writer(creator);

            ServerDriver.readAvailable(inC);
            ServerDriver.send(outC, "Creator");
            ServerDriver.readAvailable(inC);

            ServerDriver.send(outC, "1");
            ServerDriver.readAvailable(inC);
            ServerDriver.send(outC, "list-test-room");
            ServerDriver.readAvailable(inC); // подтверждение

            // Клиент 2: запрашивает список
            try (Socket viewer = ServerDriver.connect(port)) {
                BufferedReader inV  = ServerDriver.reader(viewer);
                PrintWriter    outV = ServerDriver.writer(viewer);

                ServerDriver.readAvailable(inV);
                ServerDriver.send(outV, "Viewer");
                ServerDriver.readAvailable(inV);

                ServerDriver.send(outV, "2");
                List<String> list = ServerDriver.readAvailable(inV);

                String joined = String.join(" ", list);
                assertTrue(joined.contains("list-test-room"),
                        "Список комнат должен содержать 'list-test-room'. Получено: " + list);
            }
        }
    }
}
