# Balance Service

Balance Service — это микросервис для управления балансами аккаунтов и транзакциями. Сервис предоставляет API для операций с балансом: пополнение, снятие средств, перевод средств, просмотр средств за период и просмотр баланса.

---

## Пререквизиты

Перед запуском приложения убедитесь, что на вашем компьютере установлены:

- **Java 21**  
  Проверьте с помощью команды:
  ```bash
  java -version
  ```
- **Apache Maven (версия 3.8 и выше)**  
  Проверьте с помощью команды:
  ```bash
  mvn -version
  ```
- **Docker и Docker Compose**  
  Проверьте с помощью команд:
  ```bash
  docker --version
  docker-compose --version
  ```

---

## Шаги по запуску

### 1. Клонирование репозитория

Клонируйте проект из вашего репозитория Git:

```bash
git clone https://github.com/alela-mgn/balance-service.git
cd balance-service
```

---

### 2. Настройка переменных окружения

Создайте файл `.env` в корне проекта и заполните его следующими переменными окружения:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/balance_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
```

Эти переменные используются как для локального запуска, так и для работы с Docker Compose.

---

### 3. Сборка JAR-файла

Соберите `.jar` файл приложения с помощью Maven:

```bash
mvn clean package -DskipTests
```

После успешной сборки файл будет находиться в папке `target` и называться `balance-service-0.0.1-SNAPSHOT.jar`.

---

### 4. Запуск с помощью Docker Compose

#### 4.1 Запустите Docker Compose

Для запуска приложения вместе с RabbitMQ и PostgreSQL выполните следующую команду:

```bash
docker-compose up -d
```

#### 4.2 Что произойдет?

1. **RabbitMQ**: будет доступен на `localhost:5672`.
2. **PostgreSQL**: будет доступен на `localhost:5432`.
3. **Приложение Balance Service**: будет доступно на `http://localhost:8080`.

> **Примечание**: Перед запуском убедитесь, что порты `8080`, `5672` и `5432` свободны.

#### 4.3 Остановка Docker Compose

Чтобы остановить контейнеры, используйте команду:

```bash
docker-compose down
```

---

### 5. Альтернативный локальный запуск (без Docker)

Если вы хотите запустить приложение локально, выполните следующие шаги:

#### 5.1 Запустите RabbitMQ и PostgreSQL

- **RabbitMQ**: Убедитесь, что он запущен на `localhost:5672`.  
  Используйте стандартный логин и пароль `guest` / `guest`.

- **PostgreSQL**: Запустите сервер и создайте базу данных:
  ```sql
  CREATE DATABASE balance_db;
  ```

#### 5.2 Запустите приложение

Выполните следующую команду для запуска приложения:

```bash
java -jar target/balance-service-0.0.1-SNAPSHOT.jar
```

Приложение будет доступно по адресу `http://localhost:8080`.

---

### 6. Проверка работы приложения

#### 6.1 Swagger UI

Документация API доступна по адресу:

```
http://localhost:8080/swagger-ui.html
```

#### 6.2 Тестирование API с помощью Postman

Используйте коллекцию Postman, которая находится в папке `postman/BalanceService.postman_collection.json`. Импортируйте коллекцию в Postman для тестирования всех доступных эндпоинтов.

---


