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
| Backend | http://localhost:8080 | `scripts\dev-backend.cmd` (najpierw sam startuje bazę), albo `cd backend` + `.\mvnw.cmd` |
| Frontend | http://localhost:4200 | `scripts\dev-frontend.cmd` (`npm start`, `/api` przez proxy do :8080) |

- Login: `talia` / `talia`
- Token synchronizacji: `test-token`

`.\mvnw.cmd` bez argumentów uruchamia backend z profilem `dev`. Ustawienia lokalne są w [backend/config/application-dev.yml](backend/config/application-dev.yml), który nie trafia do jara. Baza musi wtedy działać (`scripts\db-start.cmd`).

Po zmianach w `tsconfig.json` (np. nowe komponenty Spartana) zrestartuj `npm start`, bo serwer w trybie watch nie wczytuje aliasów ścieżek ponownie.

### Baza danych

Lokalny Postgres 18 działa w Dockerze ([compose.dev.yml](compose.dev.yml)) na **localhost:5433**, baza `talia`, user `talia` / `talia` (np. do DBeavera). Dane są w wolumenie Dockera, więc przetrwają restart.

| Skrypt | Co robi |
|---|---|
| `scripts\db-start.cmd` | startuje bazę i czeka, aż będzie gotowa |
| `scripts\db-seed.cmd` | wgrywa dane testowe: 8 tygodni wagi, talii, kroków, treningów i nawyków ([seed-dev.sql](scripts/seed-dev.sql)); zastępuje istniejące pomiary |
| `scripts\db-reset.cmd` | usuwa bazę razem z danymi (pyta o potwierdzenie) |

Schemat tworzy Flyway przy starcie backendu, więc przed pierwszym `db-seed` uruchom raz backend. Pełny reset z danymi testowymi: `db-reset` → `dev-backend` → `db-seed`.

Alternatywa bez lokalnej bazy: [TestTaliaApplication](backend/src/test/java/app/talia/TestTaliaApplication.java) (uruchamiana z IDE) stawia tymczasowego Postgresa przez Testcontainers.

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
