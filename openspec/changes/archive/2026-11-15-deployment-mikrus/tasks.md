# Tasks

## 1. Images and stack

- [x] 1.1 `backend/Dockerfile`, `frontend/Dockerfile`, `.dockerignore` files, theme script out of `index.html`; verify both images build locally
- [x] 1.2 `deploy/compose.prod.yml`, `deploy/Caddyfile`, `deploy/backup.sh`, `deploy/.env.example`; verify the stack starts locally from the local images, the app loads without CSP errors, registration (ALTCHA worker), login, service worker and `/api` work, and a backup file is written

## 2. CI

- [x] 2.1 `.github/workflows/ci.yml` (tests on PR/push, images on `main`); verify the workflow runs green on the pull request

## 3. Docs

- [x] 3.1 `docs/deploy.md` (step-by-step server commands), README and CLAUDE.md updated, archive the change in the same PR
