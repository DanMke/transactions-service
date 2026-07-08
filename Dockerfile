# syntax=docker/dockerfile:1

# --- Build stage ---
FROM gradle:8.14.5-jdk21 AS build
WORKDIR /app

# Resolve dependencies in their own layer so source-only changes reuse the cache
# instead of re-downloading every dependency.
COPY settings.gradle.kts build.gradle.kts ./
RUN gradle dependencies --no-daemon --quiet > /dev/null 2>&1 || true

COPY src ./src
RUN gradle bootJar --no-daemon

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
