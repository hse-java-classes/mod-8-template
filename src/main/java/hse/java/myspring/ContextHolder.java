package hse.java.myspring;

/**
 * Точка входа в MySpring-контейнер.
 */
public class ContextHolder {

    /**
     * Загрузить контекст из JSON-файла (classpath-ресурс).
     *
     * @param path путь к JSON-файлу в classpath (например {@code "context.json"})
     * @return контекст приложения с зарегистрированными бинами
     * @throws RuntimeException если файл не найден или содержит ошибки
     */
    public static ApplicationContext getApplicationContext(String path) {
        throw new UnsupportedOperationException("TODO: реализуйте загрузку контекста из JSON");
    }

    /**
     * Автоматическое сканирование компонентов ({@code @Component}) в пакете {@code hse.java}.
     *
     * @return контекст приложения с обнаруженными компонентами
     */
    public static ApplicationContext autoConfigure() {
        throw new UnsupportedOperationException("TODO: реализуйте сканирование @Component");
    }
}
