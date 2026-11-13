@echo off
rem Backend on http://localhost:8080 against the local Postgres from compose.dev.yml (port 5433).
rem Settings: backend\config\application-dev.yml. Login: talia / talia, health-sync token: test-token
call "%~dp0db-start.cmd" || exit /b 1
cd /d "%~dp0..\backend"
call .\mvnw.cmd
