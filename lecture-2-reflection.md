# Лекция 2: Reflection, Сериализация, Десериализация
## Пишем свой Spring IoC контейнер

---

## Слайд 1: Введение в Reflection API

### Зачем нужна Reflection?

Reflection (Рефлексия) — это механизм Java, позволяющий:
- **Исследовать** структуру классов в runtime (поля, методы, аннотации)
- **Динамически** создавать объекты, вызывать методы, изменять поля
- **Работать** с неизвестными заранее классами

### Примеры использования в реальных фреймворках:
- **Spring** — сканирует классы, находит аннотации `@Component`, `@Autowired`
- **Hibernate** — отображает поля Java объектов на колонки БД
- **JUnit** — находит методы с аннотацией `@Test` и запускает их
- **Jackson** — десериализует JSON в Java объекты

### Когда избегать Reflection?
- ❌ Производительность критична (reflection ~10-100x медленнее обычного вызова)
- ❌ Нужна типизация на этапе компиляции
- ❌ Можно решить генерацией кода или другими способами

### Цель лекции
Понять, как Spring работает "под капотом", реализовав собственный IoC контейнер.

---

## Слайд 2: Получение Class объекта

`Class` — это объект, представляющий класс в runtime. Это ключ ко всей Reflection API.

### Способы получения Class объекта

```java
// Способ 1: Использование .class
Class<?> stringClass = String.class;
Class<?> intClass = int.class;

// Способ 2: От объекта
String str = "hello";
Class<?> cls = str.getClass();

// Способ 3: Class.forName() - по имени класса (строка)
Class<?> cls = Class.forName("java.lang.String");

// Способ 4: ClassLoader
ClassLoader loader = Thread.currentThread().getContextClassLoader();
Class<?> cls = loader.loadClass("com.example.MyClass");
```

### Информация из Class объекта

```java
Class<?> cls = String.class;

// Имя класса
System.out.println(cls.getName());           // java.lang.String
System.out.println(cls.getSimpleName());     // String
System.out.println(cls.getPackageName());    // java.lang

// Иерархия
System.out.println(cls.getSuperclass());     // class java.lang.Object

// Интерфейсы
Class<?>[] interfaces = cls.getInterfaces(); // [Serializable, Comparable, ...]

// Модификаторы доступа
int mods = cls.getModifiers();
System.out.println(Modifier.isPublic(mods));    // true
System.out.println(Modifier.isFinal(mods));     // true

// Приватный ли класс (вложенный)
System.out.println(cls.isMemberClass());    // false
System.out.println(cls.isArray());          // false
System.out.println(cls.isInterface());      // false
System.out.println(cls.isEnum());           // false
```

---

## Слайд 3: Работа с конструкторами

### Получение конструкторов

```java
class Person {
    private String name;
    private int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public Person() {
        this.name = "Unknown";
        this.age = 0;
    }
    
    private Person(String name) {
        this.name = name;
    }
}

Class<?> cls = Person.class;

// Публичные конструкторы
Constructor<?>[] publicConstructors = cls.getConstructors();
// Все конструкторы (включая приватные)
Constructor<?>[] allConstructors = cls.getDeclaredConstructors();

for (Constructor<?> c : publicConstructors) {
    System.out.println(c);
    // public Person(java.lang.String, int)
    // public Person()
}
```

### Создание объектов через Constructor

```java
Class<?> cls = Person.class;

// Вариант 1: Конструктор без параметров
Constructor<?> constructor = cls.getConstructor();
Person p1 = (Person) constructor.newInstance();

// Вариант 2: Конструктор с параметрами
Constructor<?> constructor = cls.getConstructor(String.class, int.class);
Person p2 = (Person) constructor.newInstance("Alice", 30);

// Вариант 3: Приватный конструктор
Constructor<?> privateConstructor = cls.getDeclaredConstructor(String.class);
privateConstructor.setAccessible(true); // Важно!
Person p3 = (Person) privateConstructor.newInstance("Bob");
```

### Получение информации о конструкторе

```java
Constructor<?> ctor = Person.class.getConstructor(String.class, int.class);

// Параметры
Class<?>[] paramTypes = ctor.getParameterTypes();
// [class java.lang.String, int]

// Модификаторы
int mods = ctor.getModifiers();
System.out.println(Modifier.isPublic(mods));    // true

// Объявленные исключения
Class<?>[] exceptions = ctor.getExceptionTypes();
```

---

## Слайд 4: Работа с полями (Fields)

### Получение полей

```java
class User {
    public String username;
    private String email;
    protected int userId;
    
    public User(String username) {
        this.username = username;
    }
}

Class<?> cls = User.class;

// Только публичные поля (и наследованные)
Field[] publicFields = cls.getFields();

// ВСЕ поля, включая приватные (но не наследованные)
Field[] allFields = cls.getDeclaredFields();

for (Field f : allFields) {
    System.out.println(f.getName() + " : " + f.getType());
}
// username : class java.lang.String
// email : class java.lang.String
// userId : int
```

### Чтение и изменение значений полей

```java
User user = new User("john");
Field usernameField = User.class.getField("username");
Field emailField = User.class.getDeclaredField("email");
emailField.setAccessible(true); // Разрешить доступ к приватному полю

// Чтение значения
String name = (String) usernameField.get(user);
System.out.println(name); // john

// Изменение значения
usernameField.set(user, "jane");
System.out.println(user.username); // jane

emailField.set(user, "jane@example.com");
String email = (String) emailField.get(user);
System.out.println(email); // jane@example.com

// Примитивные типы
Field idField = User.class.getDeclaredField("userId");
idField.setAccessible(true);
idField.setInt(user, 123);
System.out.println(idField.getInt(user)); // 123
```

---

## Слайд 5: Работа с методами (Methods)

### Получение методов

```java
class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
    
    public int subtract(int a, int b) {
        return a - b;
    }
    
    private int multiply(int a, int b) {
        return a * b;
    }
    
    public String toString() {
        return "Calculator";
    }
}

Class<?> cls = Calculator.class;

// Публичные методы (включая наследованные)
Method[] publicMethods = cls.getMethods();

// ВСЕ методы, включая приватные (но не наследованные)
Method[] allMethods = cls.getDeclaredMethods();

// Конкретный метод
Method addMethod = cls.getMethod("add", int.class, int.class);
Method multiplyMethod = cls.getDeclaredMethod("multiply", int.class, int.class);
```

### Вызов методов через Reflection

```java
Calculator calc = new Calculator();

// Публичный метод
Method addMethod = Calculator.class.getMethod("add", int.class, int.class);
int result = (int) addMethod.invoke(calc, 5, 3);
System.out.println(result); // 8

// Приватный метод
Method multiplyMethod = Calculator.class.getDeclaredMethod("multiply", int.class, int.class);
multiplyMethod.setAccessible(true);
int product = (int) multiplyMethod.invoke(calc, 5, 3);
System.out.println(product); // 15

// Метод без параметров
Method toStringMethod = Calculator.class.getMethod("toString");
String str = (String) toStringMethod.invoke(calc);
System.out.println(str); // Calculator

// Статический метод
Method staticMethod = Integer.class.getMethod("parseInt", String.class);
int num = (int) staticMethod.invoke(null, "42"); // null для статических методов
System.out.println(num); // 42
```

### Получение информации о методе

```java
Method method = Calculator.class.getMethod("add", int.class, int.class);

// Имя и тип возврата
System.out.println(method.getName());           // add
System.out.println(method.getReturnType());     // int

// Параметры
Class<?>[] paramTypes = method.getParameterTypes();
// [int, int]

Parameter[] parameters = method.getParameters();
for (Parameter p : parameters) {
    System.out.println(p.getName() + " : " + p.getType());
    // a : int
    // b : int
}

// Модификаторы и исключения
int mods = method.getModifiers();
Class<?>[] exceptions = method.getExceptionTypes();
```

---

## Слайд 6: Работа с аннотациями

### Определение и использование аннотаций

```java
// Определение аннотации
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Autowired {
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Value {
    String value();
}

// Использование аннотаций
@Component("userService")
class UserService {
    @Autowired
    private UserRepository repository;
    
    @Value("${app.name}")
    private String appName;
}
```

### Чтение аннотаций через Reflection

```java
Class<?> cls = UserService.class;

// Аннотация на класс
if (cls.isAnnotationPresent(Component.class)) {
    Component comp = cls.getAnnotation(Component.class);
    String value = comp.value();
    System.out.println("Component name: " + value); // userService
}

// Все аннотации на класс
Annotation[] annotations = cls.getAnnotations();

// Аннотации на полях
for (Field field : cls.getDeclaredFields()) {
    if (field.isAnnotationPresent(Autowired.class)) {
        System.out.println("Field " + field.getName() + " needs autowiring");
    }
    
    if (field.isAnnotationPresent(Value.class)) {
        Value valueAnnotation = field.getAnnotation(Value.class);
        System.out.println("Field " + field.getName() + " has value: " + valueAnnotation.value());
    }
}

// Аннотации на методах
for (Method method : cls.getDeclaredMethods()) {
    if (method.isAnnotationPresent(PostConstruct.class)) {
        System.out.println("Method " + method.getName() + " is @PostConstruct");
    }
}
```

---

## Слайд 7: Практический пример - Simple DI контейнер

```java
public class SimpleContainer {
    private Map<String, Object> beans = new HashMap<>();
    
    /**
     * Регистрирует объект как бин по имени
     */
    public void registerBean(String name, Object bean) {
        beans.put(name, bean);
    }
    
    /**
     * Получает зарегистрированный бин
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> type) {
        return (T) beans.get(name);
    }
    
    /**
     * Автоматически сканирует класс и внедряет зависимости
     */
    public void autowire(Object bean) {
        Class<?> cls = bean.getClass();
        
        // Ищем все поля с @Autowired
        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                field.setAccessible(true);
                
                // Пытаемся найти бин такого же типа
                String beanName = field.getName();
                if (beans.containsKey(beanName)) {
                    try {
                        field.set(bean, beans.get(beanName));
                        System.out.println("Autowired " + beanName + " to " + bean.getClass().getSimpleName());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}

// Использование
public class Main {
    public static void main(String[] args) {
        SimpleContainer container = new SimpleContainer();
        
        // Регистрируем бины
        UserRepository repository = new UserRepository();
        container.registerBean("userRepository", repository);
        
        UserService service = new UserService();
        container.registerBean("userService", service);
        
        // Автоматически внедряем зависимости
        container.autowire(service);
        
        // Используем сервис
        service.doSomething();
    }
}
```

---

## Слайд 8: Сериализация в JSON

### Зачем JSON для конфигурации IoC?

JSON (JavaScript Object Notation) — отличный формат для конфигурации:
- ✅ Читаем для человека
- ✅ Структурирован и типизирован
- ✅ Легко парсить программно
- ✅ Поддерживается везде

### Пример конфигурации для MySpring

```json
{
  "beans": [
    {
      "id": "userRepository",
      "class": "com.example.repository.UserRepository"
    },
    {
      "id": "userService",
      "class": "com.example.service.UserService",
      "constructor-args": [
        {
          "ref": "userRepository"
        }
      ]
    },
    {
      "id": "appConfig",
      "class": "com.example.config.AppConfig",
      "properties": {
        "databaseUrl": "jdbc:postgresql://localhost/mydb",
        "appName": "MySpring App"
      }
    }
  ]
}
```

---

## Слайд 9: Десериализация JSON в Java объекты

### Использование Jackson

Jackson — самая популярная библиотека для работы с JSON в Java.

```java
// Зависимость в pom.xml
// <dependency>
//     <groupId>com.fasterxml.jackson.core</groupId>
//     <artifactId>jackson-databind</artifactId>
//     <version>2.17.0</version>
// </dependency>

import com.fasterxml.jackson.databind.ObjectMapper;

class BeanConfig {
    public String id;
    public String className;
    public List<String> constructorArgs;
}

public class ConfigLoader {
    private ObjectMapper mapper = new ObjectMapper();
    
    public List<BeanConfig> loadConfig(String jsonString) throws Exception {
        BeanConfig[] beans = mapper.readValue(jsonString, BeanConfig[].class);
        return Arrays.asList(beans);
    }
}

// Или читать из файла
String json = new String(Files.readAllBytes(Paths.get("config.json")));
BeanConfig[] beans = mapper.readValue(json, BeanConfig[].class);
```

### Пример более сложного JSON

```json
{
  "bean": {
    "id": "userService",
    "class": "com.example.UserService",
    "dependencies": {
      "repository": {
        "ref": "userRepository"
      }
    },
    "properties": {
      "timeout": 5000,
      "retryCount": 3,
      "enabled": true
    }
  }
}
```

```java
class BeanDefinition {
    public String id;
    public String className;
    public Map<String, String> dependencies;  // ref -> beanName
    public Map<String, Object> properties;
}

BeanDefinition bean = mapper.readValue(jsonString, BeanDefinition.class);
System.out.println(bean.id);                    // userService
System.out.println(bean.className);              // com.example.UserService
System.out.println(bean.properties.get("timeout")); // 5000
```

---

## Слайд 10: Построение IoC контейнера — часть 1

### Интерфейс контейнера

```java
public interface Container {
    /**
     * Получить бин по имени
     */
    <T> T getBean(String name);
    
    /**
     * Получить бин по типу
     */
    <T> T getBean(Class<T> type);
    
    /**
     * Проверить, зарегистрирован ли бин
     */
    boolean containsBean(String name);
}
```

### Регистрация бинов из JSON

```java
public class JsonContextContainer implements Container {
    private Map<String, Object> singletons = new HashMap<>();
    private Map<String, Class<?>> beanClasses = new HashMap<>();
    private Map<String, BeanDefinition> definitions = new HashMap<>();
    
    public JsonContextContainer(String jsonFilePath) throws Exception {
        loadFromJson(jsonFilePath);
    }
    
    private void loadFromJson(String filePath) throws Exception {
        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
        
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> config = mapper.readValue(jsonContent, Map.class);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> beansConfig = (List<Map<String, Object>>) config.get("beans");
        
        for (Map<String, Object> beanConfig : beansConfig) {
            String id = (String) beanConfig.get("id");
            String className = (String) beanConfig.get("class");
            
            // Регистрируем определение бина
            BeanDefinition definition = new BeanDefinition();
            definition.id = id;
            definition.className = className;
            definition.constructorArgs = (List<Object>) beanConfig.get("constructor-args");
            definition.properties = (Map<String, Object>) beanConfig.get("properties");
            
            definitions.put(id, definition);
            
            try {
                beanClasses.put(id, Class.forName(className));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + className, e);
            }
        }
    }
    
    @Override
    public <T> T getBean(String name) {
        // Проверяем, уже ли создан (singleton)
        if (singletons.containsKey(name)) {
            return (T) singletons.get(name);
        }
        
        // Создаём новый бин
        BeanDefinition definition = definitions.get(name);
        if (definition == null) {
            throw new RuntimeException("Bean not found: " + name);
        }
        
        Object bean = createBean(definition);
        singletons.put(name, bean);
        return (T) bean;
    }
    
    private Object createBean(BeanDefinition definition) {
        Class<?> cls = beanClasses.get(definition.id);
        
        try {
            // Вариант 1: Конструктор с параметрами
            if (definition.constructorArgs != null && !definition.constructorArgs.isEmpty()) {
                Class<?>[] paramTypes = new Class<?>[definition.constructorArgs.size()];
                Object[] args = new Object[definition.constructorArgs.size()];
                
                for (int i = 0; i < definition.constructorArgs.size(); i++) {
                    Object arg = definition.constructorArgs.get(i);
                    
                    if (arg instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> argMap = (Map<String, Object>) arg;
                        
                        if (argMap.containsKey("ref")) {
                            // Это ссылка на другой бин
                            String refName = (String) argMap.get("ref");
                            Object refBean = getBean(refName);
                            args[i] = refBean;
                            paramTypes[i] = refBean.getClass();
                        } else if (argMap.containsKey("value")) {
                            // Это простое значение
                            Object value = argMap.get("value");
                            args[i] = value;
                            paramTypes[i] = value.getClass();
                        }
                    }
                }
                
                Constructor<?> constructor = cls.getConstructor(paramTypes);
                return constructor.newInstance(args);
            }
            
            // Вариант 2: Конструктор без параметров
            Constructor<?> constructor = cls.getConstructor();
            Object bean = constructor.newInstance();
            
            // Устанавливаем properties
            if (definition.properties != null) {
                for (Map.Entry<String, Object> entry : definition.properties.entrySet()) {
                    Field field = cls.getDeclaredField(entry.getKey());
                    field.setAccessible(true);
                    field.set(bean, entry.getValue());
                }
            }
            
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean: " + definition.id, e);
        }
    }
    
    @Override
    public <T> T getBean(Class<T> type) {
        // Найти бин по типу (упрощённо)
        for (Map.Entry<String, Class<?>> entry : beanClasses.entrySet()) {
            if (type.isAssignableFrom(entry.getValue())) {
                return getBean(entry.getKey());
            }
        }
        throw new RuntimeException("Bean of type not found: " + type.getName());
    }
    
    @Override
    public boolean containsBean(String name) {
        return definitions.containsKey(name);
    }
}

// Вспомогательный класс
class BeanDefinition {
    public String id;
    public String className;
    public List<Object> constructorArgs;
    public Map<String, Object> properties;
}
```

---

## Слайд 11: Построение IoC контейнера — часть 2 (Аннотации)

### Поддержка @Component и @Autowired

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Autowired {
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentScan {
    String value();
}
```

### Контейнер с поддержкой аннотаций

```java
public class AnnotationContextContainer implements Container {
    private Map<String, Object> singletons = new HashMap<>();
    private Map<String, Class<?>> beanClasses = new HashMap<>();
    
    public AnnotationContextContainer(String scanPackage) {
        scanAndRegisterBeans(scanPackage);
    }
    
    private void scanAndRegisterBeans(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        
        try {
            URL packageURL = classLoader.getResource(path);
            if (packageURL == null) {
                throw new RuntimeException("Package not found: " + packageName);
            }
            
            File packageDir = new File(packageURL.getFile());
            
            // Рекурсивно ищем все .class файлы
            scanDirectory(packageDir, packageName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan package: " + packageName, e);
        }
    }
    
    private void scanDirectory(File directory, String packageName) {
        if (!directory.isDirectory()) {
            return;
        }
        
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = file.getName().substring(0, file.getName().length() - 6);
                String fullClassName = packageName + "." + className;
                
                try {
                    Class<?> cls = Class.forName(fullClassName);
                    
                    // Проверяем, помечена ли аннотацией @Component
                    if (cls.isAnnotationPresent(Component.class)) {
                        Component comp = cls.getAnnotation(Component.class);
                        String beanName = comp.value().isEmpty() ? 
                            uncapitalize(cls.getSimpleName()) : comp.value();
                        
                        beanClasses.put(beanName, cls);
                        System.out.println("Registered bean: " + beanName + " -> " + fullClassName);
                    }
                } catch (ClassNotFoundException e) {
                    // Пропускаем, если класс не загружен
                }
            }
        }
    }
    
    @Override
    public <T> T getBean(String name) {
        if (singletons.containsKey(name)) {
            return (T) singletons.get(name);
        }
        
        Class<?> cls = beanClasses.get(name);
        if (cls == null) {
            throw new RuntimeException("Bean not found: " + name);
        }
        
        // Создаём экземпляр
        Object bean = createInstance(cls);
        
        // Внедряем зависимости
        injectDependencies(bean);
        
        // Кэшируем как singleton
        singletons.put(name, bean);
        
        return (T) bean;
    }
    
    private Object createInstance(Class<?> cls) {
        try {
            Constructor<?> constructor = cls.getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate: " + cls.getName(), e);
        }
    }
    
    private void injectDependencies(Object bean) {
        Class<?> cls = bean.getClass();
        
        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                field.setAccessible(true);
                
                // Ищем бин того же типа
                String beanName = uncapitalize(field.getType().getSimpleName());
                if (beanClasses.containsKey(beanName)) {
                    Object dependency = getBean(beanName);
                    
                    try {
                        field.set(bean, dependency);
                        System.out.println("Injected " + beanName + " into " + bean.getClass().getSimpleName());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
    
    @Override
    public <T> T getBean(Class<T> type) {
        for (Map.Entry<String, Class<?>> entry : beanClasses.entrySet()) {
            if (type.isAssignableFrom(entry.getValue())) {
                return getBean(entry.getKey());
            }
        }
        throw new RuntimeException("Bean of type not found: " + type.getName());
    }
    
    @Override
    public boolean containsBean(String name) {
        return beanClasses.containsKey(name);
    }
    
    private String uncapitalize(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
```

---

## Слайд 12: Использование контейнера

```java
// Классы приложения
@Component("userRepository")
class UserRepository {
    public User findById(int id) {
        return new User(id, "John");
    }
}

@Component("userService")
class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public void printUser(int id) {
        User user = userRepository.findById(id);
        System.out.println("User: " + user);
    }
}

// Использование
public class Application {
    public static void main(String[] args) {
        // Инициализация контейнера
        Container container = new AnnotationContextContainer("com.example");
        
        // Получение и использование сервиса
        UserService service = container.getBean("userService", UserService.class);
        service.printUser(1);
    }
}
```

---

# ЧАСТЬ 2: ПАМЯТЬ В JAVA И REFLECTION

---

## Слайд 13: Архитектура памяти JVM

### Структура памяти JVM

JVM использует несколько областей памяти:

```
┌─────────────────────────────────────────────────┐
│             HEAP (кучa)                         │
│  [Young Gen] [Old Gen] [Permanent Gen / Metaspace] │
│  Управляется GC                                 │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│          STACK (стек вызовов)                   │
│  [Frame 1] [Frame 2] [Frame 3]                  │
│  Очищается автоматически                       │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│        METHOD AREA / Metaspace                  │
│  Структуры классов, методы, байткод             │
│  Существует во время работы приложения          │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│         DIRECT MEMORY (вне JVM)                 │
│  Прямой доступ к памяти OS                      │
└─────────────────────────────────────────────────┘
```

### Различия Java 8 и Java 9+

| Версия | Permanent Generation | Что изменилось |
|--------|---------------------|-----------------|
| Java 8 | ✅ Есть (часть Heap) | Permanent Gen имеет фиксированный размер, может переполниться |
| Java 9-25 | ❌ Заменена на Metaspace | Metaspace находится в native memory (вне JVM heap), может расти без ограничений |

**Почему это важно для Reflection?**
- Каждый раз, когда вы вызываете `Class.forName()` или используете Reflection, информация о классе хранится в Metaspace
- При неправильном использовании (например, динамическое создание классов) можно исчерпать Metaspace

---

## Слайд 14: Где хранятся Class объекты?

### Class как объект

```java
Class<?> stringClass = String.class;
```

Вопрос: Где в памяти хранится `stringClass`?

**Ответ:** Это двухуровневая система:

1. **Класс "String"** хранится в **Metaspace**
   - Структура класса: поля, методы, аннотации
   - Байткод методов
   - Таблица методов (VTable)

2. **Объект Class<String>** (который вы получаете) хранится в **Heap**
   - Это обычный Java объект типа `java.lang.Class`
   - Ссылается на структуру класса в Metaspace

```
        HEAP                          METASPACE
    ┌──────────────┐            ┌──────────────┐
    │  Class<?>    │────────┐   │ Class Info   │
    │  object      │        │   │              │
    │              │        └──→│ методы       │
    │              │            │ поля         │
    └──────────────┘            │ байткод      │
                                │ VTable       │
                                └──────────────┘
```

---

## Слайд 15: Жизненный цикл класса

### Этапы загрузки класса

```java
// Этап 1: Загрузка (Loading)
Class<?> cls = Class.forName("com.example.User"); 
// ClassLoader читает файл User.class, парсит байткод

// Этап 2: Связывание (Linking)
// - Верификация: проверка байткода
// - Подготовка: выделение памяти для статических переменных
// - Разрешение: замена символических ссылок на прямые

// Этап 3: Инициализация (Initialization)
// Выполняются статические инициализаторы
static {
    System.out.println("Class initialized");
}

// Класс полностью готов к использованию
User user = cls.getConstructor().newInstance();
```

### Классы остаются в памяти

```java
for (int i = 0; i < 1000000; i++) {
    // ОПАСНО! Каждый вызов Class.forName создаёт новый класс
    // в Metaspace, старые не удаляются
    Class<?> cls = Class.forName("com.example.gen.GeneratedClass" + i);
}
// ⚠️ Metaspace OutOfMemoryError!
```

---

## Слайд 16: Объекты на Heap - детальный взгляд

### Строение объекта в памяти

```java
class User {
    private String name;      // 8 байт (ссылка)
    private int age;          // 4 байта
    private long userId;      // 8 байт
}

// Объект в памяти выглядит примерно так:
/*
┌─────────────────────────────────────────┐
│ Object Header (12-16 байт в Java 64bit) │
│  - Mark Word (синхронизация, GC info)  │
│  - Class pointer (ссылка на структуру)  │
│  - Array length (если массив)           │
├─────────────────────────────────────────┤
│ Instance Data (поля объекта)            │
│  - name reference: 8 байт               │
│  - age: 4 байта                         │
│  - userId: 8 байт                       │
│  - padding: 4 байта (выравнивание)      │
├─────────────────────────────────────────┤
│ Итого: ~48 байт (с выравниванием)       │
└─────────────────────────────────────────┘
*/
```

### Reflection и объекты

```java
// Когда используем reflection, объект остаётся объектом
User user = new User("John", 30);
Field nameField = User.class.getDeclaredField("name");
nameField.setAccessible(true);

// Это просто другой способ доступа к полю
String name = (String) nameField.get(user);

// В памяти ничего не меняется! Объект выглядит идентично
```

---

## Слайд 17: Класс vs Объект

### Важное различие

```java
// Класс - это структура, описание
Class<?> userClass = User.class;
// Хранится в METASPACE
// Один класс на всё приложение

// Объект - это экземпляр класса
User user1 = new User("Alice", 25);
User user2 = new User("Bob", 30);
User user3 = new User("Carol", 35);
// Хранятся в HEAP
// Много объектов одного класса
```

### Доступ через Reflection

```java
User user = new User("John", 30);

// Получаем класс
Class<?> cls = user.getClass();
// или
Class<?> cls = User.class;

// Оба способа ссылаются на ОДНУ и ту же структуру класса в Metaspace

// Но сам объект `user` находится в Heap
Field nameField = cls.getDeclaredField("name");
nameField.get(user); // Ищет поле в конкретном объекте в Heap
```

---

## Слайд 18: Эволюция байткода в Java

### Java 1.0 - 1.4: Базовый байткод

```
public class Example {
    public void sum(int a, int b) {
        int result = a + b;
        System.out.println(result);
    }
}

// Байткод (упрощённо):
// ALOAD_0          ; load this
// GETFIELD System  ; get System.out
// ILOAD_1          ; load a
// ILOAD_2          ; load b
// IADD             ; add a + b
// INVOKEVIRTUAL System.out.println
// RETURN
```

### Java 5: Generics и автоупаковка

```java
// Код с генериками
List<String> strings = new ArrayList<>();
strings.add("hello");
String str = strings.get(0);

// Во время компиляции генерируется правильный байткод
// Во время выполнения (runtime) тип информация УДАЛЯЕТСЯ (type erasure)
// List strings на самом деле List<Object>

// Через reflection информация о типе теряется
Method addMethod = List.class.getMethod("add", Object.class);
// Видим Object, не String!
```

### Java 7-11: Оптимизации (invokedynamic)

```java
// Лямбды (Java 8)
list.forEach(s -> System.out.println(s));

// Генерирует специальный bytecode:
// invokedynamic #25,  0  // #25:0, count=1, args=0
// Позволяет оптимальный выбор реализации в runtime
```

### Java 15+: Records и запечатанные классы

```java
// Records (Java 16)
public record User(String name, int age) {}

// Компилируется в специальный байткод с:
// - Автоматическими equals/hashCode/toString
// - Специальными flags в class файле
// Reflection видит эти методы как обычные

public sealed class Vehicle permits Car, Truck {}

// Байткод содержит информацию о sealed модификаторе
// Class.isSealed() может это определить
```

---

## Слайд 19: JIT Compilation и Reflection

### Tiered Compilation (Java 8+)

```
Код Java
    ↓
[1] Интерпретатор (bytecode интерпретируется напрямую)
    ↓
    (код запускается часто? → профилирование)
    ↓
[2] C1 Compiler (быстрая JIT, средняя оптимизация)
    ↓
    (код очень горячий? → ещё больше профилирования)
    ↓
[3] C2 Compiler (медленная JIT, агрессивная оптимизация)
    ↓
Машинный код (очень быстро!)
```

### Reflection и JIT

```java
// Обычный вызов - JIT оптимизирует
for (int i = 0; i < 100000; i++) {
    user.getName();  // JIT может inline и оптимизировать
}

// Вызов через reflection - JIT не может оптимизировать
for (int i = 0; i < 100000; i++) {
    nameField.get(user);  // JIT не может заранее знать, какое поле
}

// Выводы:
// ❌ Reflection медленнее в горячих циклах (tight loops)
// ✅ Reflection нормально для инициализации (один раз)
```

### Пример производительности

```java
class Performance {
    private String value;
    
    public static void main(String[] args) throws Exception {
        Performance obj = new Performance();
        Field field = Performance.class.getDeclaredField("value");
        field.setAccessible(true);
        
        // Прямой доступ
        long start = System.nanoTime();
        for (int i = 0; i < 100_000_000; i++) {
            obj.value = "test";
        }
        long directTime = System.nanoTime() - start;
        
        // Через reflection
        start = System.nanoTime();
        for (int i = 0; i < 100_000_000; i++) {
            field.set(obj, "test");
        }
        long reflectionTime = System.nanoTime() - start;
        
        System.out.println("Direct: " + directTime);
        System.out.println("Reflection: " + reflectionTime);
        System.out.println("Ratio: " + (reflectionTime / directTime) + "x");
        // Результат: Reflection может быть в 5-50x медленнее!
    }
}
```

---

## Слайд 20: MethodHandle - альтернатива Reflection

### Проблема Reflection

```java
// Reflection: медленная, но гибкая
Field field = cls.getDeclaredField("name");
field.setAccessible(true);
field.set(obj, value);  // Может быть в 50x медленнее!
```

### Решение: MethodHandle (Java 7+)

```java
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

class Example {
    private String name;
    
    public static void main(String[] args) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        
        // Один раз: получить MethodHandle для поля
        MethodHandle setter = lookup.findVarHandle(Example.class, "name", String.class);
        
        Example obj = new Example();
        
        // Много раз: использовать MethodHandle (JIT оптимизирует!)
        for (int i = 0; i < 100_000_000; i++) {
            setter.set(obj, "value" + i);
        }
        
        // Результат: в 3-10x быстрее, чем Reflection!
    }
}
```

---

## Слайд 21: Class Loading и ClassLoader

### Иерархия ClassLoaders

```
         Bootstrap ClassLoader
         (загружает java.*, javax.*)
                 ↑
                 │
         Extension ClassLoader
         (загружает расширения JDK)
                 ↑
                 │
       Application ClassLoader
       (загружает ваш код)
                 ↑
                 │
      Custom ClassLoaders
      (если нужны)
```

### Delegation Model

```java
class MyClassLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 1. Спрашиваем родителя (Application ClassLoader)
        //    он спрашивает своего родителя (Extension)
        //    он спрашивает своего родителя (Bootstrap)
        
        // 2. Если не нашлось, сами ищем класс
        byte[] classBytes = loadClassBytes(name);
        
        // 3. Преобразуем байты в Class
        return defineClass(name, classBytes, 0, classBytes.length);
    }
    
    private byte[] loadClassBytes(String name) {
        // Загружаем .class файл
        // Можем модифицировать байткод!
        return null;
    }
}

// Использование
ClassLoader loader = new MyClassLoader();
Class<?> cls = loader.loadClass("com.example.MyClass");
```

### Reflection и ClassLoader

```java
// Какой ClassLoader использовать?
Class<?> cls1 = Class.forName("java.lang.String");
// Bootstrap ClassLoader

Class<?> cls2 = Class.forName("com.example.User");
// Application ClassLoader

// Явно указать ClassLoader
ClassLoader loader = Thread.currentThread().getContextClassLoader();
Class<?> cls3 = Class.forName("com.example.User", true, loader);
```

---

## Слайд 22: Метаданные класса в Metaspace

### Что хранит JVM о классе?

```
Для каждого класса в Metaspace:

┌──────────────────────────────────────────┐
│ Классовые метаданные (Class Metadata)    │
├──────────────────────────────────────────┤
│ - Имя класса                             │
│ - Родительский класс                     │
│ - Интерфейсы                             │
│ - Модификаторы (public, final, abstract) │
│ - Версия класса                          │
├──────────────────────────────────────────┤
│ Метаданные полей (Field Metadata)       │
│ - Имя, тип, модификаторы                 │
│ - Смещение в памяти объекта              │
├──────────────────────────────────────────┤
│ Метаданные методов (Method Metadata)     │
│ - Сигнатура метода                       │
│ - Модификаторы доступа                   │
│ - Аннотации                              │
│ - Таблица методов (VTable)                │
│ - Байткод (code attribute)                │
├──────────────────────────────────────────┤
│ Дополнительные данные                    │
│ - Таблица констант (Constant Pool)       │
│ - Таблица исключений                     │
│ - Таблица локальных переменных            │
│ - Информация о GC и JIT                   │
└──────────────────────────────────────────┘
```

### Доступ через Reflection

```java
Class<?> cls = User.class;

// Получаем все эти метаданные через Reflection API:
cls.getName();                    // метаданные класса
cls.getDeclaredFields();          // метаданные полей
cls.getDeclaredMethods();         // метаданные методов
cls.getAnnotations();             // аннотации класса
```

---

## Слайд 23: Синхронизация и класс Monitor

### Object Monitor

```
Каждый Java объект имеет "монитор" для синхронизации:

┌────────────────────────────────┐
│  OBJECT HEADER                 │
│  ┌──────────────────────────┐  │
│  │ Mark Word (64 bit)       │  │
│  │ - Lock state (2 бита)    │  │
│  │ - GC info                │  │
│  │ - Monitor pointer        │  │ ← для синхронизации
│  └──────────────────────────┘  │
└────────────────────────────────┘

Состояния Mark Word:
- 01: Unlocked (обычное состояние)
- 00: Lightweight lock (потокобезопасная блокировка)
- 10: Heavyweight lock (монитор объекта)
- 11: GC marker
```

### Reflection и синхронизация

```java
// Reflection работает с обычными объектами
synchronized (obj) {
    Field field = cls.getDeclaredField("value");
    field.setAccessible(true);
    field.set(obj, newValue);
}

// Reflection не создаёт специальные потокобезопасные версии
// Вы ответственны за синхронизацию!
```

---

## Слайд 24: Garbage Collection и Reflection

### Какие объекты собирает GC?

```java
Class<?> cls = Class.forName("com.example.User");
// Class объект (в Heap) + метаданные класса (в Metaspace)

User user = new User("John");
// user объект (в Heap)

// Если user больше не используется, GC может его удалить
// Но Class объект остаётся, пока есть ссылки на него
```

### Утечки памяти через Reflection

```java
// ОПАСНО: неконтролируемое создание классов
Set<Class<?>> classes = new HashSet<>();

for (int i = 0; i < Integer.MAX_VALUE; i++) {
    // Каждая попытка создать класс = новая запись в Metaspace
    Class<?> cls = Class.forName("DynamicClass" + i, true, 
        new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) {
                return super.findClass(name);
            }
        });
    classes.add(cls);  // ссылка остаётся!
}

// ⚠️ Metaspace Out of Memory
```

### Правильное использование

```java
// ХОРОШО: кэшируем Class объекты
private static final Map<String, Class<?>> classCache = new HashMap<>();

public static Class<?> getClass(String name) throws ClassNotFoundException {
    return classCache.computeIfAbsent(name, key -> {
        try {
            return Class.forName(key);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    });
}
```

---

## Слайд 25: Java 25 - инновации в Reflection

### Virtual Threads и Reflection

```java
// Java 21+: Virtual Threads (облегченные потоки)
Thread vthread = Thread.ofVirtual()
    .start(() -> {
        // Reflection работает в виртуальном потоке
        Class<?> cls = Class.forName("com.example.User");
        Object obj = cls.getConstructor().newInstance();
    });
```

###记录 Records и Reflection

```java
// Java 16+: Records (неизменяемые данные)
public record User(String name, int age) {}

// Reflection видит особые методы:
Class<?> cls = User.class;

// Компоненты записи
RecordComponent[] components = cls.getRecordComponents();
for (RecordComponent rc : components) {
    System.out.println(rc.getName() + ": " + rc.getType());
    // name: class java.lang.String
    // age: int
}

// Автоматически сгенерированные методы
Method accessor = components[0].getAccessor(); // name()
```

### Паттерны (Pattern Matching)

```java
// Java 21+: Паттерны в switch
Object obj = new User("John", 30);

switch (obj) {
    case User u when u.age() > 18 -> 
        System.out.println("Adult: " + u.name());
    case User u -> 
        System.out.println("Minor: " + u.name());
    default -> {}
}

// Внутри JVM это использует Reflection и invokedynamic
```

### Sealed Classes и Reflection

```java
// Java 17+: Запечатанные классы
sealed class Animal permits Dog, Cat {}
final class Dog extends Animal {}
final class Cat extends Animal {}

Class<?> cls = Animal.class;

// Reflection позволяет узнать подклассы
if (cls.isSealed()) {
    Class<?>[] permits = cls.getPermittedSubclasses();
    // [class Dog, class Cat]
}
```

### Vector API и структуры данных

```java
// Java 21+: SIMD операции (не прямо в Reflection, но важно)
// Reflection используется для инспекции векторных типов

// Будущее (Java 25):
// - Более эффективная работа с примитивными типами
// - Улучшения в Type Tokens
// - Лучшая поддержка generics в Reflection
```

---

## Слайд 26: Практические советы

### ✅ Когда использовать Reflection

```java
// 1. Фреймворки (Spring, Hibernate) - нужна гибкость
@Component
class MyService {}

// 2. Конфигурация из внешних источников
String className = config.getString("service.class");
Class<?> cls = Class.forName(className);

// 3. Единовременная инициализация (не в цикле)
List<Handler> handlers = scanAndInstantiate("com.example.handlers");

// 4. Работа с неизвестными типами
Object obj = mapper.readValue(json, Object.class);
Class<?> cls = obj.getClass();
```

### ❌ Когда избегать Reflection

```java
// 1. Горячие циклы (tight loops)
for (int i = 0; i < 1000000; i++) {
    method.invoke(obj, args);  // ❌ МЕДЛЕННО
}

// 2. Критичная производительность
// ✅ Используйте MethodHandle или код-генерацию вместо

// 3. Типизация в compile-time важна
// ✅ Используйте обычные вызовы, не reflection

// 4. Работа с приватным кодом других библиотек
// ❌ Тестирование внутреннего API хрупко
```

### 📊 Правила оптимизации

```java
public class OptimizedContainer {
    // Кэшируем дорогостоящие операции reflection
    private static final Map<String, Constructor<?>> constructorCache = new HashMap<>();
    private static final Map<String, Field> fieldCache = new HashMap<>();
    
    public Object createBean(String className) throws Exception {
        Constructor<?> constructor = constructorCache.computeIfAbsent(className, key -> {
            try {
                return Class.forName(key).getConstructor();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        return constructor.newInstance();
    }
    
    public void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = fieldCache.computeIfAbsent(
            obj.getClass().getName() + "." + fieldName, 
            key -> {
                try {
                    String[] parts = key.split("\\.");
                    String className = String.join(".", Arrays.copyOf(parts, parts.length - 1));
                    String fname = parts[parts.length - 1];
                    Field f = Class.forName(className).getDeclaredField(fname);
                    f.setAccessible(true);
                    return f;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );
        
        field.set(obj, value);
    }
}
```

---

## Слайд 27: Проверочный тест

### Вопросы для проверки понимания

1. **Где хранятся Class объекты в Java 17+?**
   - Ответ: В Heap (как обычные объекты), а метаданные класса - в Metaspace

2. **Почему reflection медленнее обычных вызовов?**
   - Ответ: JIT не может оптимизировать динамические вызовы, всё делается в runtime

3. **Что произойдёт, если вызвать `field.set()` на приватном поле без `setAccessible(true)`?**
   - Ответ: Выбросится исключение `IllegalAccessException`

4. **Как Spring узнаёт о классах с аннотацией @Component?**
   - Ответ: Сканирует классы в указанных пакетах, парсит аннотации через reflection

5. **Что такое Constant Pool в класс-файле?**
   - Ответ: Таблица констант - строки, числа, ссылки на классы, методы

---

## Слайд 28: Задание на закрепление

### Мини-задание

Реализуйте простой **Bean Factory**:

```java
public class BeanFactory {
    private Map<String, Class<?>> beans = new HashMap<>();
    
    /**
     * Регистрирует класс как бин
     */
    public void registerBean(String name, Class<?> clazz) {
        beans.put(name, clazz);
    }
    
    /**
     * Создаёт экземпляр, разрешая зависимости через поля
     * с аннотацией @Inject
     */
    public <T> T createBean(String name) {
        // TODO: реализовать
        // 1. Найти класс по имени
        // 2. Создать экземпляр через конструктор без параметров
        // 3. Найти все поля с @Inject
        // 4. Для каждого поля найти соответствующий бин и внедрить его
        // 5. Вернуть готовый объект
    }
}

// @Inject аннотация
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject {
    String value() default "";
}

// Классы для тестирования
class UserRepository {
    public String getUser(int id) { return "User" + id; }
}

class UserService {
    @Inject("userRepository")
    private UserRepository repo;
    
    public String getUser(int id) {
        return repo.getUser(id);
    }
}

// Использование
BeanFactory factory = new BeanFactory();
factory.registerBean("userRepository", UserRepository.class);
factory.registerBean("userService", UserService.class);

UserService service = factory.createBean("userService");
System.out.println(service.getUser(1)); // User1
```

---

## Заключение

### Ключевые моменты

- ✅ **Reflection** — мощный инструмент для фреймворков и интроспекции
- ✅ **Class объекты** в Heap, метаданные классов в Metaspace
- ✅ **Производительность**: кэшируйте результаты reflection, не используйте в tight loops
- ✅ **Java 25** продолжает улучшать отражение (records, sealed classes, virtual threads)
- ✅ **IoC контейнер** — практическое применение reflection для внедрения зависимостей

### Дальше

- Используйте эти знания для домашних заданий по MySpring
- Опробуйте MethodHandle для оптимизации
- Разберитесь, как Spring работает "под капотом"
