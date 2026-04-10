# Практика: Сервис модерации контента через Kafka

**Тег коммита:** `moderation-service`
**Баллы:** 4

---

## Описание

Расширьте существующий `posts-service` и напишите новый `moderation-service`, реализующие полный цикл ручной модерации публикаций через Kafka.

Пост создаётся со статусом `PENDING` и сразу не отображается на стене. `posts-service` публикует событие в Kafka. `moderation-service` получает событие, сохраняет пост в свою БД и показывает его модератору в веб-интерфейсе. Модератор одобряет или отклоняет пост — результат публикуется обратно в Kafka. `posts-service` получает решение и обновляет статус: одобренный пост появляется на стене, отклонённый — нет.

## API — posts-service

| Метод | Путь | Тело запроса | Тело ответа | Статус |
|-------|------|--------------|-------------|--------|
| POST | `/posts` | `{"authorId","content"}` | созданный пост со статусом `PENDING` | 201 |
| GET | `/wall` | — | список постов со статусом `APPROVED` | 200 |

## API — moderation-service (Thymeleaf UI)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/moderation` | список постов со статусом `PENDING` |
| POST | `/moderation/{postId}/approve` | одобрить пост |
| POST | `/moderation/{postId}/reject` | отклонить пост |
| GET | `/moderation/history` | все проверенные посты (`APPROVED` + `REJECTED`) |

## Требования

- Топик `content.submitted`: `posts-service` публикует событие после сохранения поста; `moderation-service` потребляет.
- Топик `content.moderated`: `moderation-service` публикует результат решения; `posts-service` потребляет и обновляет статус.
- Статусы поста: `PENDING` (не виден на стене), `APPROVED` (виден), `REJECTED` (не виден).
- У каждого сервиса своя PostgreSQL.
- Обновление статуса в `posts-service` при обработке решения модерации — в транзакции.
- `docker compose up` поднимает Zookeeper, Kafka, два экземпляра PostgreSQL и оба приложения.
- Оба сервиса: Spring Boot 3.x, Java 17+. Kafka — через Spring Kafka, потребители с `@KafkaListener`. У каждого сервиса свой `Dockerfile`. 

## Проверка

Ручной сценарий через `docker compose` или интеграционные тесты: **эндпоинт** `POST /posts` → список/действия модерации (`/moderation`, approve/reject) → **эндпоинт** `GET /wall` показывает только одобренные посты после цепочки. Для автотестов — по возможности поднять сервисы с тестовыми PostgreSQL и Kafka (например Testcontainers).
