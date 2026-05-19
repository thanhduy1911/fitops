# FitOps — Fitness Operation

A personal fitness operating system. Track kcal-in (food), kcal-out (workouts), body composition, and receive
deterministic weekly insights — all in one self-hostable platform.

---

## What it does

| Feature                | Description                                                                                               |
|------------------------|-----------------------------------------------------------------------------------------------------------|
| **Food tracking**      | Log meals from a searchable food catalog with full nutrition facts (calories, macros, vitamins, minerals) |
| **Workout tracking**   | Build plans, run sessions, log sets with weight / reps / RPE                                              |
| **Body metrics**       | Daily weight logging with rolling averages                                                                |
| **Progress dashboard** | Kcal-in vs kcal-out chart, weight trend, calorie deficit/surplus                                          |
| **Weekly insights**    | Rule-based plateau detection, consistency warnings, calorie target alerts                                 |
| **Data ownership**     | Export everything as JSON; delete your account and all data                                               |

---

## Tech Stack

| Concern    | Choice                                                   |
|------------|----------------------------------------------------------|
| Runtime    | Java 21                                                  |
| Framework  | Spring Boot 4.0.6 + Spring Modulith 2.0.6                |
| Build      | Maven                                                    |
| Database   | PostgreSQL 18 (schema-per-module)                        |
| Migrations | Flyway 10                                                |
| Auth       | Spring Security 6 + JWT (JJWT 0.13)                      |
| Mapping    | MapStruct 1.6.3                                          |
| Cache      | Caffeine (L1) → Redis (L2, Phase 2)                      |
| API docs   | SpringDoc OpenAPI 3.1                                    |
| Testing    | JUnit 6 + Mockito + Testcontainers + RestAssured         |
| Frontend   | React 18 + Vite + TypeScript + Tailwind + TanStack Query |

---

## Architecture

Modular monolith. Each module owns its PostgreSQL schema and exposes behaviour only through port interfaces or domain
events. Spring Modulith validates boundaries in CI — no module can access another module's JPA repository.

```
com.fitops/
├── shared/      # Kernel: BaseEntity, PaginatedResult, security, config
├── identity/    # Auth, users, JWT, refresh tokens
├── food/        # Food catalog, nutrition facts, meal logging (kcal-in)
├── fitness/     # Exercise library, workout plans, sessions, sets (kcal-out)
├── planning/    # Goals, meal plans
├── progress/    # Body metrics, daily calorie summaries
└── insight/     # Rule-based weekly analysis
```

For full design details, see [`docs/system-design.md`](docs/system-design.md).

---

## Local Development

### Prerequisites

- Java 21+
- Docker Desktop
- Node.js 20+

### Start the database

```bash
docker-compose up -d
```

This starts PostgreSQL on `localhost:5432` and pgAdmin on `localhost:5050`.

### Run the backend

```bash
./gradlew bootRun
```

Flyway runs automatically on startup — all schemas and tables are created.  
Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

### Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`. API requests are proxied to `localhost:8080`.

---

## Running Tests

```bash
# All tests (includes module boundary validation via Spring Modulith)
./gradlew test

# With TestContainers — requires Docker running
./gradlew test
```

---

## API Design

- `2xx` responses return the resource, page, or command result directly.
- `4xx`/`5xx` responses return RFC 7807 `ProblemDetail` with a stable `errorCode` field.

See the [error code catalog](docs/system-design.md#26-error-code-catalog) and Swagger UI for full endpoint
documentation.

---

## Module Boundary Rules

```
identity     → shared
food         → shared, identity.port.UserQueryPort
fitness      → shared, identity.port.UserQueryPort
planning     → shared, food.port.FoodQueryPort, fitness.port.FitnessQueryPort
progress     → shared; listens to food.MealLoggedEvent, fitness.SessionCompletedEvent
insight      → shared, progress.port.ProgressQueryPort, fitness.port.FitnessQueryPort
notification → shared; listens to any domain event (Phase 3)
```

Cross-module calls go through published port interfaces only — no direct repository access across module boundaries.

---

## Implementation Roadmap

| Phase | Scope                                                                                                 |
|-------|-------------------------------------------------------------------------------------------------------|
| 1     | Foundation: Gradle, Docker Compose, CI, `shared` kernel, `identity` module (auth, JWT, body stats)    |
| 2     | `food` module (catalog, nutrition, meal logging) + `fitness` module (exercises, plans, sessions)      |
| 3     | `planning` module (goals, meal plans) + `progress` module (body metrics, daily summaries) + dashboard |
| 4     | `insight` module (rule engine, weekly scheduled job) + frontend insights feed                         |
| 5     | Deployment, data export/deletion, performance baseline                                                |
| 6     | AI integration (post-MVP, separate design doc)                                                        |

---

## Project Structure

```
fitops/
├── fitops-backend/          # Spring Boot application (Gradle)
├── frontend/                # React + Vite (Phase 2)
├── docs/
│   ├── system-design.md     # Full architecture document
│   └── adr/                 # Architecture Decision Records
├── docker-compose.yml
├── CLAUDE.md                # AI agent rules for this project
└── README.md
```
