.PHONY: dev-up dev-down dev-logs dev-restart prod-up prod-down prod-logs prod-restart ps

# DEV

dev-up:
	docker compose up -d

dev-build:
	docker compose up -d --build

dev-rebuild:
	docker compose build --no-cache
	docker compose up -d

dev-down:
	docker compose down

dev-down-volumes:
	docker compose down -v

dev-restart:
	docker compose restart

dev-logs:
	docker compose logs -f

dev-pull:
	docker compose pull

# PROD

prod-up:
	docker compose -f docker-compose.yml --profile prod up -d

prod-build:
	docker compose -f docker-compose.yml --profile prod up -d --build

prod-down:
	docker compose -f docker-compose.yml --profile prod down

prod-restart:
	docker compose -f docker-compose.yml --profile prod restart

prod-logs:
	docker compose -f docker-compose.yml --profile prod logs -f

prod-pull:
	docker compose -f docker-compose.yml --profile prod pull