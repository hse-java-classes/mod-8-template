package hse.java.myspring.ioc;

import hse.java.myspring.ApplicationContext;
import hse.java.myspring.ContextHolder;
import hse.java.myspring.fixtures.Repository;
import hse.java.myspring.fixtures.SimpleBean;
import hse.java.myspring.fixtures.UserService;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Happy-path тесты IoC-контейнера из JSON. Тег: myspring-ioc-1 (3 балла из 5).
 *
 * Проверяют:
 * 1. Создание бина через no-arg конструктор и получение по id.
 * 2. Создание бина с зависимостью через ref.
 * 3. Создание бина с вложенным (inline) дескриптором.
 * 4. Запрос несуществующего id возвращает null.
 */
@Tag("myspring-ioc-1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySpringIocHappyPathTest {

    private static ApplicationContext ctx;

    @BeforeAll
    static void loadContext() {
        ctx = ContextHolder.getApplicationContext("myspring/basic-context.json");
        assertNotNull(ctx, "getApplicationContext не должен возвращать null");
    }

    // -------------------------------------------------------------------------
    // Тест 1: no-arg бин создаётся и доступен по id
    // -------------------------------------------------------------------------
    @Test
    @Order(1)
    @Timeout(5)
    @DisplayName("No-arg бин создаётся и возвращается по id")
    void noArgBeanCreated() {
        Object bean = ctx.getBean("simple");
        assertNotNull(bean, "Бин 'simple' не найден в контексте");
        assertInstanceOf(SimpleBean.class, bean,
                "Бин 'simple' должен быть экземпляром SimpleBean");

        SimpleBean simple = (SimpleBean) bean;
        assertEquals("SimpleBean", simple.getName(),
                "Метод getName() должен работать на созданном бине");
    }

    // -------------------------------------------------------------------------
    // Тест 2: бин с ref — зависимость внедрена через конструктор
    // -------------------------------------------------------------------------
    @Test
    @Order(2)
    @Timeout(5)
    @DisplayName("Бин с ref получает зависимость через конструктор")
    void refDependencyInjected() {
        Object bean = ctx.getBean("service");
        assertNotNull(bean, "Бин 'service' не найден в контексте");
        assertInstanceOf(UserService.class, bean,
                "Бин 'service' должен быть экземпляром UserService");

        UserService service = (UserService) bean;
        Repository repo = service.getRepository();
        assertNotNull(repo, "Repository должен быть внедрён в UserService через конструктор");
        assertFalse(repo.findAll().isEmpty(),
                "Внедрённый Repository должен быть функциональным");
    }

    // -------------------------------------------------------------------------
    // Тест 3: ref ссылается на тот же экземпляр бина
    // -------------------------------------------------------------------------
    @Test
    @Order(3)
    @Timeout(5)
    @DisplayName("ref ссылается на тот же объект, что и getBean по id")
    void refPointsToSameInstance() {
        Object repoDirect = ctx.getBean("repo");
        UserService service = (UserService) ctx.getBean("service");

        assertSame(repoDirect, service.getRepository(),
                "ref должен ссылаться на тот же экземпляр, что зарегистрирован по id");
    }

    // -------------------------------------------------------------------------
    // Тест 4: неизвестный id → null
    // -------------------------------------------------------------------------
    @Test
    @Order(4)
    @Timeout(5)
    @DisplayName("Запрос несуществующего id возвращает null")
    void unknownIdReturnsNull() {
        Object bean = ctx.getBean("no-such-bean");
        assertNull(bean, "getBean для несуществующего id должен возвращать null");
    }

    // -------------------------------------------------------------------------
    // Тест 5: вложенный бин (inline дескриптор в constructorArgs)
    // -------------------------------------------------------------------------
    @Test
    @Order(5)
    @Timeout(5)
    @DisplayName("Вложенный бин создаётся из inline-дескриптора в constructorArgs")
    void nestedBeanCreated() {
        ApplicationContext nestedCtx =
                ContextHolder.getApplicationContext("myspring/nested-context.json");

        Object bean = nestedCtx.getBean("serviceNested");
        assertNotNull(bean, "Бин 'serviceNested' не найден в контексте");
        assertInstanceOf(UserService.class, bean);

        UserService service = (UserService) bean;
        Repository repo = service.getRepository();
        assertNotNull(repo,
                "Вложенный Repository должен быть создан и передан в конструктор UserService");
        assertFalse(repo.findAll().isEmpty(),
                "Вложенный Repository должен быть функциональным");
    }
}
