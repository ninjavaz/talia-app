@echo off
rem Deletes the local database volume. The next backend start recreates the schema from scratch.
choice /M "Usunac wszystkie lokalne dane"
if errorlevel 2 exit /b 0
docker compose -f "%~dp0..\compose.dev.yml" down -v || exit /b 1
echo Baza usunieta. Uruchom backend, a potem ewentualnie scripts\db-seed.cmd.
