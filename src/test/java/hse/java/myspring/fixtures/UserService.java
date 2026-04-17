package hse.java.myspring.fixtures;

/**
 * Сервис с одной зависимостью — {@link Repository}.
 * Конструктор принимает Repository (для тестов ref и вложенных бинов).
 */
public class UserService {

    private final Repository repository;

    public UserService(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }
}
