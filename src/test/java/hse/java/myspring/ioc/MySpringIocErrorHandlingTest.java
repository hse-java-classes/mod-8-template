package hse.java.myspring.ioc;

import hse.java.myspring.ContextHolder;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты обработки ошибок IoC-контейнера. Тег: myspring-ioc-2 (2 балла из 5).
 *
 * Проверяют:
 * 1. Отсутствующий файл → RuntimeException.
 * 2. Несуществующий ref → RuntimeException.
 */
@Tag("myspring-ioc-2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySpringIocErrorHandlingTest {

    // -------------------------------------------------------------------------
    // Тест 1: файл не найден → RuntimeException
    // -------------------------------------------------------------------------
    @Test
    @Order(1)
    @Timeout(5)
    @DisplayName("Отсутствующий JSON-файл вызывает RuntimeException")
    void missingFileThrows() {
        assertThrows(RuntimeException.class,
                () -> ContextHolder.getApplicationContext("myspring/does-not-exist.json"),
                "getApplicationContext должен бросить RuntimeException при отсутствии файла");
    }

    // -------------------------------------------------------------------------
    // Тест 2: несуществующий ref → RuntimeException
    // -------------------------------------------------------------------------
    @Test
    @Order(2)
    @Timeout(5)
    @DisplayName("Ссылка на несуществующий бин (ref) вызывает RuntimeException")
    void missingRefThrows() {
        assertThrows(RuntimeException.class,
                () -> ContextHolder.getApplicationContext("myspring/missing-ref-context.json"),
                "getApplicationContext должен бросить RuntimeException при невалидном ref");
    }
}
