# ============================================
# ЭТАП 1: Сборка Java приложения
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Копирование pom.xml для кэширования зависимостей
COPY pom.xml .
COPY stt/pom.xml stt/
COPY stt/stt-api/pom.xml stt/stt-api/
COPY stt/stt-vosk/pom.xml stt/stt-vosk/
COPY tts/pom.xml tts/
COPY tts/tts-api/pom.xml tts/tts-api/
COPY tts/tts-rhvoice/pom.xml tts/tts-rhvoice/
COPY app/pom.xml app/

# Загрузка зависимостей (кэшируется)
RUN mvn dependency:go-offline -B || true

# Копирование исходного кода
COPY stt/ stt/
COPY tts/ tts/
COPY app/ app/

# Сборка приложения
RUN mvn clean package -DskipTests -B

# ============================================
# ЭТАП 2: Runtime образ
# ============================================
FROM eclipse-temurin:17-jre-jammy

# Установка ffmpeg для конвертации аудио
RUN apt-get update && apt-get install -y \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копирование собранного JAR
COPY --from=builder /app/app/target/app-1.0.0.jar app.jar

# Создание директории для сокетов
RUN mkdir -p /tmp/sip_prox && chmod 777 /tmp/sip_prox

# Создание директории для модели Vosk (будет монтироваться)
RUN mkdir -p /app/vosk-model

# Переменные окружения по умолчанию
ENV APP_SOCK_DIR=/tmp/sip_prox \
    VOSK_MODEL_DIR=/app/vosk-model \
    RHVOICE_BASE_URL=http://rhvoice:8080 \
    RHVOICE_VOICE=aleksandr \
    RHVOICE_FORMAT=wav \
    VOSK_MODEL_SILENCE_LAG=789

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]