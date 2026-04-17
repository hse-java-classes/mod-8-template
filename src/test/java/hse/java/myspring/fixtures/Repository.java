package hse.java.myspring.fixtures;

import java.util.List;

/**
 * Репозиторий без зависимостей (no-arg конструктор).
 * Используется как зависимость для UserService.
 */
public class Repository {

    public List<String> findAll() {
        return List.of("item-1", "item-2");
    }
}
