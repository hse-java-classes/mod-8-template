# Практика: Docker Compose окружение — Spring + PostgreSQL + Redis

**Тег коммита:** `docker-compose`
**Баллы:** 5

---

## Описание

Упакуйте Spring Boot приложение вместе с PostgreSQL и Redis так, чтобы окружение поднималось одной командой `docker compose up`, без ручных шагов. Приложение в сети compose обращается к БД и Redis по **именам сервисов**, не через `localhost` хост-машины.

## Критерии приёмки

- **Dockerfile:** multi-stage — сборка JAR (Maven), финальный слой на **JRE**, не JDK.
- **Compose:** три сервиса — приложение, PostgreSQL, Redis; переменные подключения задаются через `environment`.
- PostgreSQL с **healthcheck** (`pg_isready` или эквивалент); приложение стартует после готовности БД (`depends_on` с ожиданием health, в версии compose где это поддерживается).
- Данные PostgreSQL в **именованном volume**, переживают перезапуск контейнера.
- Внутри `docker-compose.yml` нет `localhost` / `127.0.0.1` как адреса других сервисов.
- `Dockerfile` и `docker-compose.yml` в корне репозитория.
- Версии образов зафиксированы: `postgres:16`, `redis:7`, образ JRE 21 (не JDK, не `latest`).
