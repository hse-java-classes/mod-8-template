package hse.java.lectures.network.http;

import java.io.*;
import java.net.*;

/**
 * Шаг 1: смотрим, что браузер/curl присылает на самом деле.
 * Запустите, откройте http://localhost:8080 — и прочитайте вывод в консоли.
 * HTTP — это просто текст поверх TCP.
 */
public class GetHttpRequest {

    public static void main(String[] args) throws Exception {
        int port = 8080;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Слушаем http://localhost:" + port);

            while (true) {
                Socket client = server.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                System.out.println("=== Запрос от " + client.getRemoteSocketAddress() + " ===");

                // Читаем заголовки до пустой строки (HTTP-разделитель заголовков и тела)
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    System.out.println(line);
                }

                System.out.println("=== Конец заголовков ===\n");

                // Клиент ждёт ответа — без ответа браузер зависнет
                // Просто закрываем соединение (браузер покажет ошибку, но запрос мы увидели)
                client.close();
            }
        }
    }
}
