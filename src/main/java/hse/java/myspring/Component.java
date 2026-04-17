package hse.java.myspring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает класс как компонент, который будет автоматически
 * обнаружен и зарегистрирован в контексте при вызове
 * {@link ContextHolder#autoConfigure()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
}
