@echo off
rem Backend on http://localhost:8080 with a throwaway Postgres in Docker (Testcontainers).
rem Login: talia / talia, health-sync token: test-token
docker info >nul 2>&1 || (echo Docker nie dziala - uruchom Docker Desktop. & exit /b 1)
cd /d "%~dp0..\backend"
call .\mvnw.cmd spring-boot:test-run
