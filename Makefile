.PHONY: help build run test docker-up docker-down clean tailwind-build tailwind-watch

help:
	@echo "Özerler Mermer ERP - Build and Automation Commands"
	@echo "  make build          - Build backend application with Maven"
	@echo "  make test           - Run backend unit and integration tests"
	@echo "  make run            - Run Spring Boot app locally"
	@echo "  make tailwind-build - Compile Tailwind CSS 4 assets"
	@echo "  make tailwind-watch - Watch and compile Tailwind CSS during development"
	@echo "  make docker-up      - Start full stack (App + PostgreSQL 16) via Docker Compose"
	@echo "  make docker-down    - Stop Docker Compose services"
	@echo "  make clean          - Clean Maven target and temporary assets"

build:
	mvn clean package -DskipTests

test:
	mvn test

run:
	mvn spring-boot:run

tailwind-build:
	cd frontend && npm install && npm run build

tailwind-watch:
	cd frontend && npm run dev

docker-up:
	docker compose -f docker/docker-compose.yml up -d --build

docker-down:
	docker compose -f docker/docker-compose.yml down

clean:
	mvn clean
