package hse.java.lectures.network.telnet;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Шаг 3: рассылка всем подключённым клиентам.
 * Общий список writers — потокобезопасный, потому что потоки добавляют/удаляют одновременно.
 */
public class Broadcast {

    // CopyOnWriteArrayList: чтение без блокировки, запись копирует массив — подходит когда
    // читают часто (рассылка), пишут редко (connect/disconnect)
    static final List<PrintWriter> writers = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {
        int port = 9090;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Слушаем порт " + port);

            while (true) {
                Socket client = server.accept(); // блокирующая операция
                new Thread(() -> handle(client)).start();
            }
        }
    }

    static void handle(Socket client) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(client.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            writers.add(out);

            String line;
            while ((line = in.readLine()) != null) {
                // Рассылаем всем, кроме отправителя
                String message = line;
                for (PrintWriter w : writers) {
                    if (w != out) w.println(message);
                }
            }

        } catch (IOException e) {
            System.out.println("Клиент отвалился: " + e.getMessage());
        } finally {
            // Удаляем writer при отключении, иначе продолжим писать в закрытый сокет
            if (out != null) writers.remove(out);
            try { client.close(); } catch (IOException ignored) {}
        }
    }
}
