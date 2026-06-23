# AGENTS.md

## Project overview

Fox is a monorepo for a claims management system (POC Sinistros). It contains a Spring Boot backend (Java 21) and a React frontend (Vite 8). The project deploys to a VPS running Dokploy via Docker Compose.

## Repository structure

```
fox/
├── apps/
│   ├── backend/       # Spring Boot 3.2.5 (Java 21, Maven)
│   └── frontend/      # React 19 + Vite 8 + ESLint
├── packages/          # Shared code (future use)
├── .github/workflows/ # CI pipeline
├── docker-compose.yml # Multi-service orchestration for Dokploy
├── turbo.json         # Turborepo build config
└── package.json       # npm workspaces root
```

## Development workflow

The branching strategy follows **dev → uat → main** (promotion-based):

1. **`dev`** — Active development. All feature work starts here.
2. **`uat`** — User acceptance testing. Promoted from dev when ready for QA.
3. **`main`** — Production. Promoted from uat after approval. Pushes to main trigger automatic deployment.

After committing to `main`, always sync the other branches:

```bash
git checkout dev && git merge main && git push origin dev
git checkout uat && git merge main && git push origin uat
```

## Setup commands

```bash
# Root workspace
npm install

# Frontend only
cd apps/frontend && npm install

# Backend (requires Java 21 + Maven)
cd apps/backend && mvn clean package -DskipTests -B
```

## Dev server

```bash
# Frontend dev server (port 5173)
npm run dev:frontend

# Backend (run separately)
cd apps/backend && mvn spring-boot:run

# Full stack via Docker
docker compose up --build
```

## Build commands

```bash
# Build frontend
cd apps/frontend && npm run build

# Build backend
cd apps/backend && mvn clean package -DskipTests -B

# Build all via Turborepo
npm run build
```

## Lint commands

```bash
# Frontend lint
cd apps/frontend && npm run lint
```

## Testing instructions

- The CI pipeline runs in `.github/workflows/ci.yml`.
- Backend: `mvn clean package -DskipTests -B` in `apps/backend`.
- Frontend: `npm ci && npm run build` in `apps/frontend`.
- Both must pass before merge. The deploy job only runs on pushes to `main`.

## Code style

- **Frontend**: ESLint with react-hooks and react-refresh plugins. Config in `apps/frontend/eslint.config.js`.
- **Backend**: Standard Java conventions. Spring Boot annotations. REST controllers return JSON.
- No comments unless explicitly requested.
- All files use LF line endings.

## Deployment

Deployed via **Dokploy** (Docker Compose) on a VPS. Each environment (dev, uat, prod) runs its own Dokploy application pointing to the respective branch.

- Build type: **Docker Compose**
- Docker Compose path: `docker-compose.yml`
- Frontend service: `frontend`, internal port `80`
- Backend service: `backend`, internal port `8080`
- Nginx proxies `/api/*` to the backend service
- No host ports exposed — Dokploy routes via Traefik by domain

### Domains

| Environment | Frontend | Backend API |
|---|---|---|
| dev | `fox.dev.nfptech.com.br` | `fox.dev.api.nfptech.com.br` |
| uat | `fox.uat.nfptech.com.br` | `fox.uat.api.nfptech.com.br` |
| prod | `fox.nfptech.com.br` | `fox.api.nfptech.com.br` |

### GitHub secrets (required for CI deploy)

- `VPS_HOST` — VPS IP or hostname
- `VPS_USER` — SSH username
- `VPS_SSH_KEY` — SSH private key
- `DEPLOY_PATH` — Project path on the VPS

## Commit guidelines

- Commit messages in English, concise, focused on the "why".
- Always sync all three branches after merging to `main`.
- Never push directly to `main` without passing CI checks.

## Architecture notes

- The frontend is a single-page app with mock data (no real API integration yet).
- The backend exposes `GET /` and `GET /api/health` returning `{"status":"UP","service":"fox-backend"}`.
- Nginx serves the frontend static files and reverse-proxies `/api/` to the backend.
- Docker Compose uses an internal `app-network` for inter-service communication.
- No `container_name` or host `ports` in docker-compose.yml — avoids conflicts when running multiple environments on the same VPS.
