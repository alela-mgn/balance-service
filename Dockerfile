# Используем OpenJDK 21
FROM openjdk:21-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR файл в контейнер
COPY target/balance-service-0.0.1-SNAPSHOT.jar balance-service.jar

# Указываем порт, на котором будет доступно приложение
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "balance-service.jar"]