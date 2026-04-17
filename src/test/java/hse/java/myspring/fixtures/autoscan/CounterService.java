package hse.java.myspring.fixtures.autoscan;

import hse.java.myspring.Component;

/**
 * Компонент без зависимостей. Обнаруживается по {@code @Component}.
 */
@Component
public class CounterService {

    public int count() {
        return 42;
    }
}
