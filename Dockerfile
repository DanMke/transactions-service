# syntax=docker/dockerfile:1

FROM gradle:8.14.5-jdk21 AS build
WORKDIR /app

COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar

USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
