# Web image: Caddy serving the Angular build and proxying /api to the backend. Build context: repository root
# (docker build -f frontend/Dockerfile .), so it can take deploy/Caddyfile.
FROM node:24-alpine AS build
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npx ng build --configuration production

FROM caddy:2-alpine
COPY deploy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /src/dist/frontend/browser /srv
