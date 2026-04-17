package hse.java.myspring.fixtures.autoscan;

import hse.java.myspring.Autowired;
import hse.java.myspring.Component;

/**
 * Компонент с {@code @Autowired} на конструкторе — зависимость от {@link CounterService}.
 */
@Component
public class ReportService {

    private final CounterService counterService;

    @Autowired
    public ReportService(CounterService counterService) {
        this.counterService = counterService;
    }

    public CounterService getCounterService() {
        return counterService;
    }
}
