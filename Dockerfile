# syntax=docker/dockerfile:1

# --- Build stage ---
FROM gradle:8.10-jdk21 AS build
WORKDIR /app

COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src

RUN gradle bootJar --no-daemon

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
