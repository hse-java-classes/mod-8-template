package hse.java.practice.chatstomp.di;

import org.springframework.stereotype.Component;

/**
 * Сервис, который зависит от UserRepository.
 *
 * Конструкторное внедрение зависимостей (Constructor Injection):
 * Spring видит конструктор с параметром типа UserRepository,
 * находит бин этого типа в контексте (UserRepository)
 * и автоматически передаёт его в конструктор.
 *
 * Сравнение с MySpring:
 * В MySpring вы писали:
 * {
 *   "id": "service",
 *   "class": "com.example.UserService",
 *   "constructorArgs": [
 *     { "ref": "repository" }
 *   ]
 * }
 *
 * В Spring это выглядит так — просто параметр конструктора, тип говорит сам за себя.
 * Spring разберётся автоматически.
 *
 * Почему конструкторное внедрение хорошо:
 * 1. Зависимость явна (видна в сигнатуре конструктора)
 * 2. Объект immutable — repo не изменится после создания
 * 3. Легко тестировать — в unit тесте подаёте mock из параметра
 */
@Component
public class UserService {
    private final UserRepository repository;

    // Spring внедрит UserRepository сюда
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public void registerUser(String name, String email) {
        // Валидация
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        repository.save(name, email);
    }

    public UserRepository.User getUser(int id) {
        UserRepository.User user = repository.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        return user;
    }
}
