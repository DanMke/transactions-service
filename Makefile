# Makefile for transactions-service
#
# Prerequisites:
#   - Docker Desktop running (required for `test`, `run`, `up`)
#   - A JDK 21 on PATH (required for local Gradle targets: build, jar, test, run)
#     Not needed for `up` / `up-d`, which build inside Docker.
#
# On Windows, `make` is not bundled. Install it once with either:
#   choco install make        (Chocolatey)
#   scoop install make        (Scoop)
# and run these targets from PowerShell, cmd, or Git Bash.

# Use the Windows batch wrapper on Windows, the shell wrapper elsewhere.
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

.DEFAULT_GOAL := help

.PHONY: help build jar test test-unit retest clean db-up db-down run up up-d down logs ps

help: ## Show this help
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
	@echo   make clean      - Clean Gradle build output and tear down Docker

build: ## Compile and build the project (runs tests)
	$(GRADLEW) build

jar: ## Build the executable Spring Boot jar without running tests
	$(GRADLEW) bootJar

test: ## Run the full test suite (unit + Testcontainers integration; needs Docker)
	$(GRADLEW) test

test-unit: ## Run only the pure unit tests (no Docker required)
	$(GRADLEW) test --tests "*OperationTypeTest"

retest: ## Re-run the full test suite, bypassing Gradle's up-to-date checks
	$(GRADLEW) test --rerun-tasks

run: db-up ## Start Postgres and run the app locally with bootRun
	$(GRADLEW) bootRun

db-up: ## Start only Postgres and wait until it is healthy
	$(COMPOSE) up -d --wait postgres

db-down: ## Stop the Postgres container
	$(COMPOSE) stop postgres

up: ## Build and run app + Postgres in Docker (foreground)
	$(COMPOSE) up --build

up-d: ## Build and run app + Postgres in Docker (detached)
	$(COMPOSE) up --build -d

down: ## Stop and remove the Docker stack (and volumes)
	$(COMPOSE) down -v

logs: ## Follow logs from the Docker stack
	$(COMPOSE) logs -f

ps: ## Show the status of the Docker stack
	$(COMPOSE) ps

clean: ## Clean Gradle output and tear down Docker
	$(GRADLEW) clean
	$(COMPOSE) down -v
