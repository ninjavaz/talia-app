# Talia

Prosta apka (PWA) do śledzenia talii, trendu wagi, aktywności i nawyków. Bez liczenia kalorii.

- `backend/`: Spring Boot 4.1, Java 25, PostgreSQL, Flyway
- `frontend/`: Angular 22, Tailwind 4, Spartan UI (helm w `src/app/ui`), PWA
- `scripts/`: skrypty do lokalnego uruchamiania (Windows)

Szczegóły i decyzje projektowe: [CLAUDE.md](CLAUDE.md).

## Uruchomienie lokalne

Wymagania: JDK 25, Node 24+, Docker Desktop (uruchomiony).

```bat
scripts\dev.cmd
```

Skrypt otwiera dwa okna:

| | Adres | Skrypt |
|---|---|---|
| Backend | http://localhost:8080 | `scripts\dev-backend.cmd` (`mvnw spring-boot:test-run`) |
| Frontend | http://localhost:4200 | `scripts\dev-frontend.cmd` (`npm start`, `/api` przez proxy do :8080) |

Backend sam stawia tymczasowego Postgresa w Dockerze (Testcontainers), więc nie trzeba niczego instalować ani konfigurować. Dane znikają po zamknięciu backendu.

- Login: `talia` / `talia`
- Token synchronizacji: `test-token`

Test synchronizacji:

```bash
curl -H "Authorization: Bearer test-token" -H "Content-Type: application/json" \
  -d '{"steps":[{"date":"2026-11-13","value":5321}]}' http://localhost:8080/api/health-sync
```

## Testy

```bat
cd backend && .\mvnw.cmd verify      :: testy integracyjne (wymaga Dockera)
cd frontend && npm test -- --watch=false
```
