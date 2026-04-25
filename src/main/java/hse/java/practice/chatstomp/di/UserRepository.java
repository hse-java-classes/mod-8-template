package hse.java.practice.chatstomp.di;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Хранилище пользователей в памяти.
 *
 * @Component — аннотация Spring, которая:
 * 1. Говорит Spring "это бин, создай экземпляр"
 * 2. Регистрирует класс в ApplicationContext под именем userRepository (первая буква в нижнем регистре)
 *
 * Сравнение с MySpring:
 * - В MySpring вы писали в JSON: { "id": "repository", "class": "com.example.UserRepository" }
 * - В Spring аннотация @Component делает это автоматически
 */
@Component
public class UserRepository {
    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    public void save(String name, String email) {
        users.put(nextId, new User(nextId, name, email));
        nextId++;
    }

    public User findById(int id) {
        return users.get(id);
    }

    public record User(int id, String name, String email) {}
}
