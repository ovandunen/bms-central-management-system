.PHONY: help e2e-build e2e-up e2e-test e2e-logs e2e-down

.DEFAULT_GOAL := help

help:
	@echo "Available commands:"
	@echo "  help       Show this help message"
	@echo "  e2e-build  Build the application jar and Docker image"
	@echo "  e2e-up     Start the E2E stack"
	@echo "  e2e-test   Run E2E smoke tests"
	@echo "  e2e-logs   Follow CSMS application logs"
	@echo "  e2e-down   Stop the E2E stack and remove volumes"

e2e-build:
	./mvnw clean package -DskipTests
	docker build -f src/main/docker/Dockerfile.jvm -t solar-csms-app:latest .

e2e-up:
	docker compose -f docker-compose.e2e.yml up -d

e2e-test:
	docker compose -f docker-compose.e2e.yml up -d --force-recreate --wait wiremock-ki-e2e
	docker compose -f docker-compose.e2e.yml up e2e-tester --abort-on-container-exit --exit-code-from e2e-tester

e2e-logs:
	docker compose -f docker-compose.e2e.yml logs -f solar-csms-app

e2e-down:
	docker compose -f docker-compose.e2e.yml down -v
