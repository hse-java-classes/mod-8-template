package hse.java.practice.chatstomp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Глобальный обработчик исключений.
 *
 * @RestControllerAdvice — аннотация Spring, которая:
 * 1. Делает этот класс доступным для всех контроллеров в приложении
 * 2. Методы с @ExceptionHandler перехватывают исключения нужного типа
 *
 * Без этого класса контроллер вернёт стандартный 500 ответ.
 * С этим классом мы возвращаем красивый JSON с ошибкой.
 *
 * Сравнение:
 * - Без обработчика: HTTP 500, стандартная Spring ошибка
 * - С обработчиком: HTTP 400, { "error": "Name cannot be empty", "status": 400 }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        ErrorResponse response = new ErrorResponse(
            400,
            e.getMessage(),
            System.currentTimeMillis()
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        ErrorResponse response = new ErrorResponse(
            500,
            "Internal server error: " + e.getMessage(),
            System.currentTimeMillis()
        );
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }

    record ErrorResponse(int status, String error, long timestamp) {}
}
