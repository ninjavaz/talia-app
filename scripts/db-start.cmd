@echo off
rem Starts the local Postgres (compose.dev.yml) and waits until it accepts connections.
docker info >nul 2>&1 || (echo Docker nie dziala - uruchom Docker Desktop. & exit /b 1)
docker compose -f "%~dp0..\compose.dev.yml" up -d --wait || exit /b 1
echo Postgres: localhost:5433, baza talia, user talia / talia
echo Maile (Mailpit): http://localhost:8025
