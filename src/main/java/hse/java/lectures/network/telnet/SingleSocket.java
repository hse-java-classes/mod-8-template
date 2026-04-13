package hse.java.lectures.network.telnet;

import java.io.*;
import java.net.*;

/**
 * Шаг 1: один клиент.
 * Сервер принимает одно соединение и эхо-отвечает на каждую строку.
 * Проблема: пока клиент подключён, сервер больше никого не принимает.
 */
public class SingleSocket {

    public static void main(String[] args) throws Exception {
        int port = 9090;

        // Открываем серверный сокет — он слушает входящие подключения
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Слушаем порт " + port);

            // accept() блокируется до первого подключения
            Socket client = server.accept();
            System.out.println("Клиент подключился: " + client.getRemoteSocketAddress());

            // Обёртки для удобного чтения строк и записи строк
            BufferedReader in  = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter   out  = new PrintWriter(client.getOutputStream(), true);

            String line;
            // readLine() возвращает null при разрыве соединения
            while ((line = in.readLine()) != null) {
                out.println("Echo: " + line);
                System.out.printf("Получено от %s сообщение: %s\n", client.getInetAddress(), line);
            }

            System.out.println("Клиент отключился");
        }
    }
}
