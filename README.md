# 🛒 Shop Backend

Микросервисный backend интернет-магазина, написанный на **Kotlin + Ktor**. Состоит из четырёх независимых сервисов, взаимодействующих через REST и очередь сообщений RabbitMQ.

---

## Архитектура

```
┌──────────────────────────────────────────────────────┐
│                      Клиент                          │
└──────┬──────────────┬────────────────┬───────────────┘
       │              │                │
  :8081│         :8082│           :8083│
┌──────▼──────┐ ┌─────▼──────┐ ┌──────▼──────┐
│ auth-service│ │product-svc │ │ order-svc   │
│ JWT / BCrypt│ │ Redis cache│ │RabbitMQ pub │
└──────┬──────┘ └─────┬──────┘ └──────┬──────┘
       │              │                │
       └──────────────┴────────┬───────┘
                               │
                    ┌──────────▼──────────┐
                    │   PostgreSQL 15      │
                    │   Redis 7            │
                    │   RabbitMQ 3.12      │
                    └─────────────────────┘
                               │ order.events
                    ┌──────────▼──────────┐
                    │ notification-worker  │
                    │  (email stub)        │
                    └─────────────────────┘
```

| Сервис | Порт | Описание |
|--------|------|----------|
| `auth-service` | 8081 | Регистрация, вход, JWT-токены |
| `product-service` | 8082 | CRUD товаров, кэш в Redis |
| `order-service` | 8083 | Создание/отмена заказов, статистика |
| `notification-worker` | — | Воркер очереди RabbitMQ (email-заглушка) |

---

## Технологический стек

| Слой | Технологии |
|------|------------|
| Язык / Runtime | Kotlin 1.9, JVM 17 |
| HTTP-фреймворк | Ktor 2.3.7 (Netty) |
| ORM | Exposed 0.44 |
| База данных | PostgreSQL 15 |
| Кэш | Redis 7 (Lettuce) |
| Брокер сообщений | RabbitMQ 3.12 |
| Аутентификация | JWT (java-jwt 4.4), BCrypt |
| Миграции | Flyway 9 |
| Сериализация | kotlinx.serialization |
| Логирование | Logback + kotlin-logging |
| Тесты | Ktor Test Host, MockK, Testcontainers |
| Документация | Swagger UI (OpenAPI 3.0) |
| Сборка | Gradle + Shadow JAR |
| Контейнеры | Docker + Docker Compose |

---

## Быстрый старт

### Требования

- **Docker** и **Docker Compose** (для запуска всей инфраструктуры)
- **JDK 17+** (только для локальной разработки без Docker)

### Запуск через Docker Compose

```bash
# 1. Клонировать репозиторий
git clone https://github.com/SenseiSayaka/shop-backend
cd shop-backend

# 2. (Опционально) задать свой JWT-секрет
export JWT_SECRET=my-super-secret-key-at-least-32-chars

# 3. Собрать и запустить все сервисы
docker compose up -d --build

# 4. Проверить, что всё запустилось
docker compose ps
```

После запуска сервисы будут доступны:

| URL | Что там |
|-----|---------|
| http://localhost:8081/swagger | Swagger UI — Auth Service |
| http://localhost:8082/swagger | Swagger UI — Product Service |
| http://localhost:8083/swagger | Swagger UI — Order Service |
| http://localhost:15672 | RabbitMQ Management UI (guest/guest) |

### Локальная разработка (без Docker)

```bash
# Поднять только инфраструктуру
docker compose up -d postgres redis rabbitmq

# Запустить нужный сервис (например, auth-service)
cd auth-service
./gradlew run
```

Переменные окружения по умолчанию:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/shopdb
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=your-super-secret-key-change-in-production
REDIS_URL=redis://localhost:6379
RABBITMQ_HOST=localhost
```

---

## API Overview

Все эндпоинты описаны в Swagger UI. Краткий обзор:

### Auth Service (`:8081`)

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| POST | `/auth/register` | Публичный | Регистрация нового пользователя |
| POST | `/auth/login` | Публичный | Вход, возвращает JWT |
| GET | `/auth/health` | Публичный | Health check |

### Product Service (`:8082`)

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| GET | `/products` | Публичный | Список всех товаров |
| GET | `/products/{id}` | Публичный | Товар по ID |
| POST | `/products` | Admin | Создать товар |
| PUT | `/products/{id}` | Admin | Обновить товар |
| DELETE | `/products/{id}` | Admin | Удалить товар |
| GET | `/products/health` | Публичный | Health check |

### Order Service (`:8083`)

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| POST | `/orders` | User | Создать заказ |
| GET | `/orders` | User | История заказов |
| DELETE | `/orders/{id}` | User | Отменить свой заказ |
| GET | `/stats/orders` | Admin | Статистика заказов |
| GET | `/orders/health` | Публичный | Health check |

### Аутентификация

Все защищённые эндпоинты требуют заголовок:

```
Authorization: Bearer <JWT_TOKEN>
```

Токен получается через `POST /auth/login`. Токен содержит `userId` и `role` (`user` или `admin`).

---

## Тестирование

```bash
# Тесты конкретного сервиса
cd auth-service && ./gradlew test

# Все сервисы из корня
./gradlew :auth-service:test :product-service:test :order-service:test
```

> Интеграционные тесты используют **Testcontainers** — Docker должен быть запущен.

| Файл | Тип | Что проверяет |
|------|-----|---------------|
| `AuthServiceTest` | Unit (MockK) | Валидация, регистрация, вход |
| `AuthRouteE2ETest` | E2E (Testcontainers) | HTTP-эндпоинты auth с реальной БД |
| `ProductServiceTest` | Unit (MockK) | Валидация создания товара |
| `ProductRouteTest` | Integration | HTTP-эндпоинты товаров |
| `OrderServiceTest` | Unit (MockK) | Создание и отмена заказов |
| `OrderIntegrationTest` | Integration (Testcontainers) | Репозиторий заказов с реальной БД |

---

## Структура проекта

```
shop-backend/
├── docker-compose.yml
├── settings.gradle.kts
├── auth-service/
│   └── src/
│       ├── main/kotlin/com/shop/auth/
│       │   ├── config/        # DB, Security, Serialization
│       │   ├── domain/        # Models, Tables, Exceptions
│       │   ├── plugins/       # Swagger
│       │   ├── repository/
│       │   ├── routes/
│       │   ├── service/
│       │   └── Application.kt
│       ├── main/resources/openapi/auth.yaml
│       └── test/kotlin/com/shop/auth/
├── product-service/
│   └── src/
│       ├── main/kotlin/com/shop/product/  ...
│       └── test/kotlin/com/shop/product/
├── order-service/
│   └── src/
│       ├── main/kotlin/com/shop/order/    ...
│       └── test/kotlin/com/shop/order/
└── notification-worker/
    └── src/main/kotlin/com/shop/notification/
        └── Application.kt    # RabbitMQ consumer
```

---

## Переменные окружения

### auth-service

| Переменная | Описание |
|------------|----------|
| `DATABASE_URL` | JDBC URL PostgreSQL |
| `DB_USER` / `DB_PASSWORD` | Реквизиты БД |
| `JWT_SECRET` | Секрет подписи JWT (мин. 32 символа) |

### product-service

| Переменная | Описание |
|------------|----------|
| `DATABASE_URL` / `DB_USER` / `DB_PASSWORD` | БД |
| `JWT_SECRET` | Для верификации токенов |
| `REDIS_URL` | URL Redis, например `redis://redis:6379` |

### order-service

| Переменная | Описание |
|------------|----------|
| `DATABASE_URL` / `DB_USER` / `DB_PASSWORD` | БД |
| `JWT_SECRET` | Для верификации токенов |
| `REDIS_URL` | URL Redis |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | RabbitMQ |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | Реквизиты RabbitMQ |
| `PRODUCT_SERVICE_URL` | URL product-service |

### notification-worker

| Переменная | Описание |
|------------|----------|
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | RabbitMQ |

---

## Примеры запросов

### Регистрация

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"secret123"}'
# {"token":"eyJ...","userId":1,"role":"user"}
```

### Вход

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"secret123"}'
```

### Получить все товары

```bash
curl http://localhost:8082/products
```

### Создать заказ

```bash
curl -X POST http://localhost:8083/orders \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":1,"quantity":2}]}'
```

---
