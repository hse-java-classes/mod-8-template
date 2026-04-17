package hse.java.myspring.annotations;

import hse.java.myspring.ApplicationContext;
import hse.java.myspring.ContextHolder;
import hse.java.myspring.fixtures.autoscan.CounterService;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты сканирования @Component. Тег: myspring-annotations-1 (3 балла из 7).
 *
 * Проверяют:
 * 1. autoConfigure() находит компонент по типу.
 * 2. Найденный компонент функционален.
 * 3. Запрос отсутствующего типа бросает RuntimeException.
 */
@Tag("myspring-annotations-1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySpringAnnotationsComponentTest {

    private static ApplicationContext ctx;

    @BeforeAll
    static void autoConfigureContext() {
        ctx = ContextHolder.autoConfigure();
        assertNotNull(ctx, "autoConfigure() не должен возвращать null");
    }

    // -------------------------------------------------------------------------
    // Тест 1: @Component-класс обнаружен и доступен по типу
    // -------------------------------------------------------------------------
    @Test
    @Order(1)
    @Timeout(5)
    @DisplayName("@Component обнаружен и доступен через getBean(Class)")
    void componentFoundByType() {
        CounterService counter = ctx.getBean(CounterService.class);
        assertNotNull(counter,
                "CounterService помечен @Component — должен быть найден через getBean(Class)");
    }

    // -------------------------------------------------------------------------
    // Тест 2: обнаруженный компонент функционален
    // -------------------------------------------------------------------------
    @Test
    @Order(2)
    @Timeout(5)
    @DisplayName("Обнаруженный компонент работает корректно")
    void componentIsFunctional() {
        CounterService counter = ctx.getBean(CounterService.class);
        assertEquals(42, counter.count(),
                "Метод count() должен возвращать 42");
    }

    // -------------------------------------------------------------------------
    // Тест 3: запрос класса без @Component → RuntimeException
    // -------------------------------------------------------------------------
    @Test
    @Order(3)
    @Timeout(5)
    @DisplayName("getBean(Class) для незарегистрированного типа бросает RuntimeException")
    void unknownTypeThrows() {
        assertThrows(RuntimeException.class,
                () -> ctx.getBean(String.class),
                "getBean(Class) должен бросить RuntimeException для типа без @Component");
    }
}
