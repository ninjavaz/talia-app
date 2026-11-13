@echo off
rem Starts backend and frontend in two windows.
start "Talia backend" cmd /k "%~dp0dev-backend.cmd"
start "Talia frontend" cmd /k "%~dp0dev-frontend.cmd"
echo Backend: http://localhost:8080  Frontend: http://localhost:4200  (login: talia / talia)
