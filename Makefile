# Force cmd.exe on Windows so recipes are consistent even when Git Bash's `sh`
# is on PATH (otherwise `make` would run recipes under sh and fail to find the
# .bat wrapper in the current directory).
ifeq ($(OS),Windows_NT)
	SHELL := cmd.exe
	GRADLEW := .\gradlew.bat
else
	GRADLEW := ./gradlew
endif

COMPOSE := docker compose
OBS_COMPOSE := docker compose -f docker-compose.yml -f docker-compose.observability.yml

.DEFAULT_GOAL := help

.PHONY: help build jar test test-unit retest clean db-up db-down run up up-d down logs ps observability-up observability-up-d observability-down observability-logs observability-ps

help:
	@echo Available targets:
	@echo   make build      - Compile and build the project (runs the tests too)
	@echo   make jar        - Build the executable Spring Boot jar (no tests)
	@echo   make test       - Run all tests (unit + integration; needs Docker)
	@echo   make test-unit  - Run only the fast unit tests (no Docker needed)
	@echo   make retest     - Re-run all tests, ignoring Gradle's up-to-date cache
	@echo   make run        - Start Postgres, then run the app locally (bootRun)
	@echo   make db-up      - Start only the Postgres container and wait until healthy
	@echo   make db-down    - Stop the Postgres container
	@echo   make up         - Build and run the full stack (app + Postgres) in Docker
	@echo   make up-d       - Same as 'up' but detached (in the background)
	@echo   make down       - Stop and remove the Docker stack
	@echo   make logs       - Follow logs of the Docker stack
	@echo   make ps         - Show status of the Docker stack
	@echo   make observability-up    - Run app + Postgres + Prometheus + Grafana
	@echo   make observability-up-d  - Run observability stack detached
	@echo   make observability-down  - Stop and remove observability stack
	@echo   make observability-logs  - Follow observability stack logs
	@echo   make observability-ps    - Show observability stack status
	@echo   make clean      - Clean Gradle build output and tear down Docker

build:
	$(GRADLEW) build

jar:
	$(GRADLEW) bootJar

test:
	$(GRADLEW) test

test-unit:
	$(GRADLEW) test --tests "*OperationTypeTest" --tests "*AccountTest" --tests "*TransactionTest" --tests "*TransactionServiceTest"

retest:
	$(GRADLEW) test --rerun-tasks

run: db-up
	$(GRADLEW) bootRun

db-up:
	$(COMPOSE) up -d --wait postgres

db-down:
	$(COMPOSE) stop postgres

up:
	$(COMPOSE) up --build

up-d:
	$(COMPOSE) up --build -d

down:
	$(COMPOSE) down -v

logs:
	$(COMPOSE) logs -f

ps:
	$(COMPOSE) ps

observability-up:
	$(OBS_COMPOSE) up --build

observability-up-d:
	$(OBS_COMPOSE) up --build -d

observability-down:
	$(OBS_COMPOSE) down -v

observability-logs:
	$(OBS_COMPOSE) logs -f

observability-ps:
	$(OBS_COMPOSE) ps

clean:
	$(GRADLEW) clean
	$(COMPOSE) down -v
