package hse.java.myspring;

/**
 * Контекст приложения MySpring — хранит бины и предоставляет к ним доступ.
 */
public interface ApplicationContext {

    /**
     * Получить бин по строковому идентификатору.
     * Возвращает {@code null}, если бин с таким id не зарегистрирован.
     */
    Object getBean(String id);

    /**
     * Получить бин по типу (для задания myspring-annotations).
     * По умолчанию бросает {@link UnsupportedOperationException}.
     */
    default <T> T getBean(Class<T> type) {
        throw new UnsupportedOperationException("getBean(Class) не реализован");
    }
}
