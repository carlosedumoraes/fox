# Fox

POC Sinistros — Claims management system.

## Structure

```
fox/
├── apps/
│   ├── backend/     # Spring Boot (Java 21)
│   └── frontend/    # React 19 + Vite 8
├── packages/        # Shared code (future)
├── docker-compose.yml
├── turbo.json
└── package.json
```

## Quick start

```bash
npm install                          # Install root workspace
cd apps/frontend && npm install      # Install frontend deps
npm run dev:frontend                 # Start frontend dev server
cd apps/backend && mvn spring-boot:run  # Start backend
docker compose up --build            # Full stack via Docker
```

## Deploy

Three environments on the same VPS via Dokploy (Docker Compose):

| Env | Branch | Frontend | API |
|---|---|---|---|
| dev | `dev` | `fox.dev.nfptech.com.br` | `fox.dev.api.nfptech.com.br` |
| uat | `uat` | `fox.uat.nfptech.com.br` | `fox.uat.api.nfptech.com.br` |
| prod | `main` | `fox.nfptech.com.br` | `fox.api.nfptech.com.br` |

Push to `main` triggers CI + auto-deploy. See [AGENTS.md](./AGENTS.md) for full details.
