package hse.java.practice.chatstomp.di;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST контроллер, который использует UserService.
 *
 * Цепочка зависимостей:
 * UserController → (внедряет) → UserService → (внедряет) → UserRepository
 *
 * Spring при создании UserController:
 * 1. Видит в конструкторе параметр типа UserService
 * 2. Для создания UserService видит параметр типа UserRepository
 * 3. Создаёт UserRepository
 * 4. Создаёт UserService(repository)
 * 5. Создаёт UserController(service)
 *
 * Всё автоматически. Это мощь Spring DI контейнера.
 */
@RestController
class UserController {
    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    void register(@RequestBody RegisterRequest request) {
        userService.registerUser(request.name(), request.email());
    }

    @GetMapping("/user/{id}")
    UserResponse getUser(@PathVariable int id) {
        UserRepository.User user = userService.getUser(id);
        return new UserResponse(user.id(), user.name(), user.email());
    }

    record RegisterRequest(String name, String email) {}
    record UserResponse(int id, String name, String email) {}
}
