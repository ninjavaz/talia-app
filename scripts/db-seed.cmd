@echo off
rem Fills the local database with ~8 weeks of sample data (replaces existing measurements/activity/habit checks).
rem The schema is created by Flyway, so start the backend at least once before seeding.
call "%~dp0db-start.cmd" || exit /b 1
docker compose -f "%~dp0..\compose.dev.yml" exec -T db psql -q -v ON_ERROR_STOP=1 -U talia -d talia < "%~dp0seed-dev.sql" || exit /b 1
echo Dane testowe wgrane.
