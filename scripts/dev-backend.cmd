@echo off
rem Backend on http://localhost:8080 against the local Postgres from compose.dev.yml (port 5433).
rem Login: talia / talia, health-sync token: test-token
call "%~dp0db-start.cmd" || exit /b 1

set DB_URL=jdbc:postgresql://localhost:5433/talia
set DB_USER=talia
set DB_PASSWORD=talia
set TALIA_USERNAME=talia
set TALIA_PASSWORD_HASH={noop}talia
rem sha256("test-token")
set TALIA_SYNC_TOKEN_SHA256=4c5dc9b7708905f77f5e5d16316b5dfb425e68cb326dcd55a860e90a7707031e
set COOKIE_SECURE=false

cd /d "%~dp0..\backend"
call .\mvnw.cmd spring-boot:run
