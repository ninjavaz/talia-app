@echo off
rem Frontend on http://localhost:4200, /api is proxied to the backend on :8080.
cd /d "%~dp0..\frontend"
if not exist node_modules call npm install
call npm start
