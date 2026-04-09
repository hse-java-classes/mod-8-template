package hse.java.echochat;

import hse.java.util.ServerDriver;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Нагрузочный тест эхо-чата. Тег: echo-chat-4 (2 балла из 8).
 */
@Tag("echo-chat-4")
class EchoChatLoadTest {

    private static final String SERVER_CLASS = "hse.java.practice.echochat.EchoChatServer";
    private static final int CLIENTS = 10;
    private static final int MESSAGES_PER_CLIENT = 5;
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        port = ServerDriver.freePort();
        ServerDriver.startServer(SERVER_CLASS, port);
        ServerDriver.waitReady(port);
    }

    @Test
    @Timeout(15)
    @DisplayName("10 участников: все сообщения доходят, никто не получает своё")
    void tenClientsChatWithoutLoss() throws Exception {
        Socket[] sockets = new Socket[CLIENTS];
        BufferedReader[] readers = new BufferedReader[CLIENTS];
        PrintWriter[] writers = new PrintWriter[CLIENTS];

        for (int i = 0; i < CLIENTS; i++) {
            sockets[i] = ServerDriver.connect(port);
            readers[i] = ServerDriver.reader(sockets[i]);
            writers[i] = ServerDriver.writer(sockets[i]);
        }

        for (int i = 0; i < CLIENTS; i++) {
            ServerDriver.readAvailable(readers[i]);
            ServerDriver.send(writers[i], "User" + i);
        }

        Thread.sleep(600);
        for (int i = 0; i < CLIENTS; i++) {
            ServerDriver.readAvailable(readers[i]);
        }

        AtomicBoolean collecting = new AtomicBoolean(true);
        @SuppressWarnings("unchecked")
        List<String>[] received = new List[CLIENTS];
        Thread[] readerThreads = new Thread[CLIENTS];

        for (int i = 0; i < CLIENTS; i++) {
            received[i] = Collections.synchronizedList(new ArrayList<>());
            final int idx = i;
            readerThreads[i] = new Thread(() -> {
                while (collecting.get()) {
                    try {
                        String line = readers[idx].readLine();
                        if (line != null) received[idx].add(line);
                    } catch (IOException ignored) {
                    }
                }
            });
            readerThreads[i].setDaemon(true);
            readerThreads[i].start();
        }

        ExecutorService senders = Executors.newFixedThreadPool(CLIENTS);
        for (int i = 0; i < CLIENTS; i++) {
            final int idx = i;
            senders.submit(() -> {
                for (int m = 0; m < MESSAGES_PER_CLIENT; m++) {
                    ServerDriver.send(writers[idx], "msg-" + idx + "-" + m);
                }
            });
        }

        senders.shutdown();
        senders.awaitTermination(5, TimeUnit.SECONDS);

        Thread.sleep(1000);
        collecting.set(false);
        for (Thread t : readerThreads) {
            t.join(1000);
        }

        int expectedPerClient = (CLIENTS - 1) * MESSAGES_PER_CLIENT;
        int expectedTotal = CLIENTS * expectedPerClient;

        int totalReceived = 0;
        for (int i = 0; i < CLIENTS; i++) {
            totalReceived += received[i].size();
        }

        assertEquals(expectedTotal, totalReceived,
                "Ожидалось " + expectedTotal + " сообщений суммарно, получено " + totalReceived);

        for (int i = 0; i < CLIENTS; i++) {
            String ownPrefix = "msg-" + i + "-";
            for (String line : received[i]) {
                assertFalse(line.contains(ownPrefix),
                        "User" + i + " не должен получать своё сообщение, но получил: " + line);
            }
        }

        for (Socket s : sockets) s.close();
    }
}
