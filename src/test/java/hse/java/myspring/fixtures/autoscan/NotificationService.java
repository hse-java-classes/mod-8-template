package hse.java.myspring.fixtures.autoscan;

import hse.java.myspring.Autowired;
import hse.java.myspring.Component;

/**
 * Компонент с {@code @Autowired} на поле — зависимость от {@link CounterService}.
 */
@Component
public class NotificationService {

    @Autowired
    private CounterService counterService;

    public CounterService getCounterService() {
        return counterService;
    }
}
