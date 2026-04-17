package hse.java.myspring.annotations;

import hse.java.myspring.ApplicationContext;
import hse.java.myspring.ContextHolder;
import hse.java.myspring.fixtures.autoscan.CounterService;
import hse.java.myspring.fixtures.autoscan.NotificationService;
import hse.java.myspring.fixtures.autoscan.ReportService;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты внедрения зависимостей через @Autowired. Тег: myspring-annotations-2 (4 балла из 7).
 *
 * Проверяют:
 * 1. @Autowired на поле — зависимость внедрена.
 * 2. @Autowired на конструкторе — зависимость внедрена.
 * 3. Внедрённые зависимости функциональны.
 * 4. Один и тот же экземпляр зависимости при инъекции в разные компоненты.
 */
@Tag("myspring-annotations-2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySpringAnnotationsInjectionTest {

    private static ApplicationContext ctx;

    @BeforeAll
    static void autoConfigureContext() {
        ctx = ContextHolder.autoConfigure();
        assertNotNull(ctx, "autoConfigure() не должен возвращать null");
    }

    // -------------------------------------------------------------------------
    // Тест 1: @Autowired на поле — зависимость внедрена
    // -------------------------------------------------------------------------
    @Test
    @Order(1)
    @Timeout(5)
    @DisplayName("@Autowired на private-поле: зависимость внедрена")
    void fieldInjectionWorks() {
        NotificationService notification = ctx.getBean(NotificationService.class);
        assertNotNull(notification,
                "NotificationService должен быть в контексте");

        CounterService injected = notification.getCounterService();
        assertNotNull(injected,
                "@Autowired поле counterService должно быть заполнено контейнером");
    }

    // -------------------------------------------------------------------------
    // Тест 2: @Autowired на конструкторе — зависимость внедрена
    // -------------------------------------------------------------------------
    @Test
    @Order(2)
    @Timeout(5)
    @DisplayName("@Autowired на конструкторе: зависимость внедрена")
    void constructorInjectionWorks() {
        ReportService report = ctx.getBean(ReportService.class);
        assertNotNull(report,
                "ReportService должен быть в контексте");

        CounterService injected = report.getCounterService();
        assertNotNull(injected,
                "@Autowired конструктор должен получить CounterService от контейнера");
    }

    // -------------------------------------------------------------------------
    // Тест 3: внедрённые зависимости функциональны
    // -------------------------------------------------------------------------
    @Test
    @Order(3)
    @Timeout(5)
    @DisplayName("Внедрённые зависимости работают корректно")
    void injectedDependenciesAreFunctional() {
        NotificationService notification = ctx.getBean(NotificationService.class);
        assertEquals(42, notification.getCounterService().count(),
                "Внедрённый CounterService должен возвращать 42 из count()");

        ReportService report = ctx.getBean(ReportService.class);
        assertEquals(42, report.getCounterService().count(),
                "Внедрённый CounterService должен возвращать 42 из count()");
    }

    // -------------------------------------------------------------------------
    // Тест 4: одна зависимость — один экземпляр (singleton)
    // -------------------------------------------------------------------------
    @Test
    @Order(4)
    @Timeout(5)
    @DisplayName("Контейнер внедряет один и тот же экземпляр зависимости")
    void singletonDependency() {
        CounterService direct = ctx.getBean(CounterService.class);
        NotificationService notification = ctx.getBean(NotificationService.class);
        ReportService report = ctx.getBean(ReportService.class);

        assertSame(direct, notification.getCounterService(),
                "CounterService из getBean и из @Autowired-поля должны быть одним объектом");
        assertSame(direct, report.getCounterService(),
                "CounterService из getBean и из @Autowired-конструктора должны быть одним объектом");
    }
}
