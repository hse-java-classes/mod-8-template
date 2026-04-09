package hse.java.util;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Утилиты для запуска сервера студента и подключения тестовых клиентов.
 */
public final class ServerDriver {

    private ServerDriver() {}
    private static final AtomicReference<Throwable> STARTUP_ERROR = new AtomicReference<>();

    /** Найти свободный порт, чтобы не хардкодить. */
    public static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    /**
     * Запустить main() класса студента в daemon-треде.
     * JVM завершится после тестов не дожидаясь сервера.
     */
    public static Thread startServer(String className, int port) {
        STARTUP_ERROR.set(null);
        Thread t = new Thread(() -> {
            try {
                Class<?> cls = Class.forName(className);
                Method main = cls.getMethod("main", String[].class);
                main.invoke(null, (Object) new String[]{String.valueOf(port)});
            } catch (Throwable e) {
                STARTUP_ERROR.set(e);
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * Ждать пока сервер начнёт принимать соединения (до 10 секунд).
     */
    public static void waitReady(int port) throws Exception {
        for (int i = 0; i < 100; i++) {
            Throwable startupError = STARTUP_ERROR.get();
            if (startupError != null) {
                throw new IllegalStateException("Сервер упал при старте: " + startupError, startupError);
            }
            try (Socket probe = new Socket("localhost", port)) {
                probe.setSoTimeout(50);
                return;
            } catch (IOException e) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("Сервер не поднялся на порту " + port);
    }

    /** Подключить клиента с таймаутом чтения 2 секунды. */
    public static Socket connect(int port) throws IOException {
        Socket s = new Socket("localhost", port);
        s.setSoTimeout(400);
        return s;
    }

    public static BufferedReader reader(Socket s) throws IOException {
        return new BufferedReader(new InputStreamReader(s.getInputStream()));
    }

    public static PrintWriter writer(Socket s) throws IOException {
        return new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
    }

    /**
     * Читать строки пока они приходят (или пока не наступит таймаут сокета).
     * Возвращает всё прочитанное — удобно для проверки меню/приветствия.
     */
    public static List<String> readAvailable(BufferedReader reader) {
        List<String> lines = new ArrayList<>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException ignored) {
            // SocketTimeoutException — сервер ждёт ввода, это нормально
        }
        return lines;
    }

    /** Прочитать одну строку. */
    public static String readLine(BufferedReader reader) throws IOException {
        return reader.readLine();
    }

    /** Отправить строку серверу. */
    public static void send(PrintWriter writer, String line) {
        writer.println(line);
    }
}
