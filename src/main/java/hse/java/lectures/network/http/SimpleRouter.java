package hse.java.lectures.network.http;

import java.io.*;
import java.net.*;
import java.time.*;

/**
 * Шаг 3: роутинг по методу и пути.
 * Парсим первую строку запроса: "GET /path HTTP/1.1"
 * Именно это делает любой веб-фреймворк под капотом.
 *
 * Попробуйте:
 *   curl http://localhost:8080/
 *   curl http://localhost:8080/time
 *   curl http://localhost:8080/unknown
 */
public class SimpleRouter {

    public static void main(String[] args) throws Exception {
        int port = 8080;

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Слушаем http://localhost:" + port);

            while (true) {
                Socket client = server.accept();
                new Thread(() -> handle(client)).start();
            }
        }
    }

    static void handle(Socket client) {
        try (client;
             BufferedReader in  = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream   out = client.getOutputStream()) {

            // Первая строка: "GET /path HTTP/1.1"
            String requestLine = in.readLine();
            if (requestLine == null) return;

            String[] parts  = requestLine.split(" ");
            String   method = parts[0];          // GET, POST, ...
            String   path   = parts[1];          // /path

            // Дочитываем заголовки, чтобы не засорять следующий запрос
            while (!in.readLine().isEmpty()) { /* пропускаем */ }

            System.out.println(method + " " + path);

            // Роутинг
            if ("GET".equals(method) && "/".equals(path)) {
                send(out, 200, "text/html",
                        "<h1>Добро пожаловать!</h1><p><a href='/time'>Текущее время</a></p>");

            } else if ("GET".equals(method) && "/time".equals(path)) {
                send(out, 200, "text/plain", LocalDateTime.now().toString());

            } else {
                send(out, 404, "text/plain", "404 Not Found: " + path);
            }

        } catch (IOException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    static void send(OutputStream out, int status, String contentType, String body) throws IOException {
        String statusText = status == 200 ? "OK" : "Not Found";
        byte[] bodyBytes  = body.getBytes();

        String headers = "HTTP/1.1 " + status + " " + statusText + "\r\n"
                + "Content-Type: " + contentType + "; charset=utf-8\r\n"
                + "Content-Length: " + bodyBytes.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";

        out.write(headers.getBytes());
        out.write(bodyBytes);
        out.flush();
    }
}
