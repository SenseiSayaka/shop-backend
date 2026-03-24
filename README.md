# Shop Backend — Микросервисный backend на Ktor

## Сервисы

| Сервис | Порт | Описание |
|--------|------|----------|
| auth-service | 8081 | Регистрация и авторизация |
| product-service | 8082 | Управление товарами |
| order-service | 8083 | Заказы и статистика |
| notification-worker | — | Обработка событий из очереди |

## Быстрый запуск

### Требования
- Docker & Docker Compose
- JDK 17+ (для разработки)

### Запуск
```bash
# Клонировать
git clone https://github.com/SenseiSayaka/shop-backend
cd shop-backend

# Запустить все сервисы
docker compose up -d

# Проверить статус
docker compose ps# trigger ci
