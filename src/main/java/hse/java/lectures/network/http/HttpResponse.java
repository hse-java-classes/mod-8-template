package hse.java.lectures.network.http;

import java.io.*;
import java.net.*;

/**
 * Шаг 2: отвечаем корректным HTTP-ответом.
 * Структура ответа:
 *   HTTP/1.1 <код> <статус>\r\n
 *   Заголовок: значение\r\n
 *   \r\n
 *   <тело>
 *
 * Ключевые моменты:
 *   - разделитель — \r\n, не просто \n
 *   - Content-Length — обязателен, иначе клиент не знает, когда закончилось тело
 *   - пустая строка обязательна между заголовками и телом
 */
public class HttpResponse {

    public static void main(String[] args) throws Exception {
        int port = 8080;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Слушаем http://localhost:" + port);

            while (true) {
                Socket client = server.accept();

                // Читаем запрос (иначе некоторые браузеры не покажут ответ)
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                while (!in.readLine().isEmpty()) { /* пропускаем заголовки */ }

                // Формируем ответ
                String body = "Hello, World!";
                String response = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/plain; charset=utf-8\r\n"
                        + "Content-Length: " + body.getBytes().length + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"       // <-- обязательная пустая строка
                        + body;

                OutputStream out = client.getOutputStream();
                out.write(response.getBytes());
                out.flush();
                client.close();
            }
        }
    }
}
