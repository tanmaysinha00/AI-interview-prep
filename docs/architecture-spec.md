# System Architecture Prompt — AI-Powered Interview Preparation Platform

> **Role:** Act as a senior system architect, AI platform engineer, and technical interviewer with deep experience designing scalable, production-grade AI systems on the Java/Spring ecosystem.

---

## Objective

Design a complete, production-ready architecture for an AI-powered interview preparation platform that simulates real-world technical interviews. The system must allow candidates to select topics, control question count, dynamically adjust difficulty based on performance, and receive structured AI-generated questions and evaluations suitable for direct UI rendering.

---

## Technology Stack (Mandatory)

| Layer              | Technology                                                                 |
|--------------------|---------------------------------------------------------------------------|
| **Frontend**       | React 18+ (Vite), TypeScript, Tailwind CSS, Axios, React Router v6       |
| **Frontend Hosting** | GitHub Pages (static SPA with client-side routing via `HashRouter`)     |
| **Backend**        | Java 21, Spring Boot 3.x, Spring WebFlux (reactive) or Spring MVC       |
| **Security**       | Spring Security 6 + JWT (access + refresh tokens), OAuth2 (Google/GitHub login) |
| **Database**       | PostgreSQL 16 (via Spring Data JPA / Hibernate 6)                        |
| **Caching**        | Redis 7 (via Spring Data Redis — session cache, question cache, rate-limit buckets) |
| **AI Integration** | Anthropic Claude API (claude-sonnet-4-20250514), called via Spring `WebClient` or `RestClient` with reactive backpressure |
| **AOP / Logging**  | Spring AOP for cross-cutting concerns (request logging, performance metrics, exception handling) |
| **Build / CI-CD**  | Maven 3.9+ (`pom.xml`), Maven Wrapper (`mvnw`), GitHub Actions (CI/CD pipeline to Heroku) |
| **Backend Hosting**| Heroku (Eco dynos + Heroku Postgres + Heroku Redis)                      |
| **Monitoring**     | Micrometer + Prometheus metrics endpoint, structured JSON logging (Logback + Logstash encoder), Sentry for error tracking |
| **API Docs**       | SpringDoc OpenAPI 3 (Swagger UI at `/swagger-ui.html`)                   |

---

## Architecture Requirements

### 1. System Architecture

Provide a full component diagram (describe it for draw.io reproduction) covering:

- **React SPA** (GitHub Pages) → communicates with backend via REST API over HTTPS
- **Spring Boot API Gateway layer** — controllers, DTOs, validation (`jakarta.validation`)
- **Service layer** — business logic, interview orchestration, difficulty engine
- **AI Client layer** — dedicated `ClaudeApiClient` service wrapping Anthropic REST API calls with retry logic (Spring Retry / Resilience4j), circuit breaker, and timeout configuration
- **Repository layer** — Spring Data JPA repositories for PostgreSQL
- **Redis layer** — caching generated questions, rate-limit counters per user, session state for active interviews
- **Spring Security filter chain** — JWT authentication filter, CORS config for GitHub Pages origin
- **Spring AOP aspects** — `@LogExecutionTime`, `@AuditLog`, `@HandleExceptions` custom annotations

Include the request flow: `React → CORS → JWT Filter → Controller → Service → ClaudeApiClient / Repository → Response DTO → JSON`

### 2. Interview Flow Design

Design the complete interview lifecycle:

#### a) Topic Selection
- Endpoint: `POST /api/v1/interviews` with body: `{ "topics": ["spring-boot", "system-design", "java-concurrency"], "questionCount": 10, "difficulty": "MEDIUM" }`
- Backend validates topics against an enum/reference table, creates an `Interview` entity (status: `IN_PROGRESS`), returns `interviewId`

#### b) Question Generation
- Endpoint: `GET /api/v1/interviews/{id}/next-question`
- Backend selects the next topic from the pool, constructs a Claude API prompt, parses the structured JSON response, persists the `Question` entity, and returns it to the frontend
- Claude must return questions in a **strict JSON schema** (no markdown, no prose wrapping) — enforce via system prompt instructions
- Support question types: `MCQ`, `SHORT_ANSWER`, `SCENARIO`, `CODE_REVIEW`, `HANDS_ON_CODING`, `SYSTEM_DESIGN`

#### c) Answer Submission & Evaluation
- Endpoint: `POST /api/v1/interviews/{id}/questions/{qId}/answer` with body: `{ "answer": "..." }`
- Backend sends the candidate's answer + original question + rubric back to Claude for evaluation
- Claude returns: `{ "score": 0-10, "feedback": "...", "correctAnswer": "...", "conceptsCovered": [...], "improvementAreas": [...] }`

#### d) Adaptive Difficulty
- After every 2–3 questions, the `DifficultyEngine` service recalculates difficulty based on:
  - Rolling average score (last 3 questions)
  - Time taken per answer (tracked via frontend timestamps)
  - Streak detection (3 correct in a row → increase, 2 wrong in a row → decrease)
- Difficulty levels: `EASY → MEDIUM → HARD → EXPERT` — passed as a parameter in the Claude prompt
- Log every difficulty transition via AOP `@AuditLog`

#### e) Interview Summary
- Endpoint: `GET /api/v1/interviews/{id}/summary`
- Returns: overall score, per-topic breakdown, difficulty progression chart data, strengths, weaknesses, and a personalized study plan (generated by Claude in one final summarization call)

### 3. AI Integration — Claude API Specifics

Define the following in detail:

#### a) Prompt Architecture
- **System prompt:** Role assignment ("You are a senior technical interviewer..."), output format enforcement ("Respond ONLY with valid JSON matching this schema: {...}"), and constraints (no markdown fences, no conversational filler)
- **User prompt template:** Dynamically assembled per question — includes topic, difficulty, question type, and any context from previous questions to avoid repetition
- **Evaluation prompt template:** Includes the original question, expected answer rubric, candidate's answer, and scoring criteria

#### b) Response Parsing
- Claude response MUST be parsed as JSON — use Jackson `ObjectMapper` with `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false`
- Implement a `ClaudeResponseParser` service with fallback: if JSON parsing fails, retry the API call once with a stricter prompt; if still invalid, return a graceful error to the frontend
- Validate parsed response against the expected schema using `jakarta.validation` on the deserialized DTO

#### c) Token & Cost Management
- Track token usage per interview session (log `input_tokens` and `output_tokens` from Claude API response headers)
- Set a per-interview token budget (e.g., 50,000 tokens max) — reject further questions if exceeded
- Use Redis to enforce per-user rate limits: max 5 interviews/hour, max 100 API calls/day

### 4. JSON Schema — Question/Answer Format for UI

Provide the exact JSON contract the frontend will consume. It must include:

```json
{
  "questionId": "uuid",
  "interviewId": "uuid",
  "sequenceNumber": 1,
  "topic": "spring-boot",
  "difficulty": "MEDIUM",
  "type": "SCENARIO",
  "question": {
    "title": "Short title for UI card header",
    "body": "Full question text with scenario description",
    "codeSnippet": "optional Java/config code block or null",
    "hints": ["hint1", "hint2"],
    "options": ["only for MCQ type, otherwise null"],
    "timeEstimateSeconds": 300
  },
  "metadata": {
    "conceptsTested": ["dependency-injection", "bean-lifecycle"],
    "relatedTopics": ["spring-context", "aop"]
  }
}
```

And the evaluation response:

```json
{
  "questionId": "uuid",
  "score": 8,
  "maxScore": 10,
  "verdict": "CORRECT | PARTIALLY_CORRECT | INCORRECT",
  "feedback": {
    "summary": "One-line verdict",
    "detailed": "Paragraph explanation",
    "correctApproach": "Model answer or approach",
    "commonMistakes": ["mistake1"],
    "followUpSuggestion": "What to study next"
  },
  "difficultyAdjustment": "STAY | INCREASE | DECREASE"
}
```

### 5. Database Schema

Design the PostgreSQL schema with these entities (include column types, constraints, indexes):

- `users` — id (UUID), email, password_hash, provider (LOCAL/GOOGLE/GITHUB), created_at, last_login
- `interviews` — id (UUID), user_id (FK), status (ENUM: CREATED, IN_PROGRESS, COMPLETED, ABANDONED), topics (JSONB), config (JSONB — question count, initial difficulty), total_score, started_at, completed_at
- `questions` — id (UUID), interview_id (FK), sequence_number, topic, difficulty, type, question_payload (JSONB), created_at
- `answers` — id (UUID), question_id (FK), user_answer (TEXT), evaluation_payload (JSONB), score, time_taken_seconds, answered_at
- `token_usage` — id, interview_id (FK), input_tokens, output_tokens, model, created_at

Include indexes on: `interviews.user_id`, `questions.interview_id`, `answers.question_id`, `interviews.status`

### 6. Spring AOP & Cross-Cutting Concerns

Define these custom aspects:

- **`@LogExecutionTime`** — Logs method entry, exit, and execution time in milliseconds. Applied to all service-layer methods.
- **`@AuditLog`** — Logs business events (interview started, difficulty changed, interview completed) with user context. Writes to a structured audit log.
- **`@HandleExceptions`** — Global exception handler aspect + `@ControllerAdvice` with `ProblemDetail` (RFC 7807) error responses
- **`@RateLimited`** — Custom annotation backed by Redis (Lua script for atomic increment + TTL)
- **Request/Response logging** — Servlet filter or `CommonsRequestLoggingFilter` with body masking for sensitive fields (passwords, tokens)

All logs must be structured JSON (Logback + Logstash encoder) for easy parsing by log aggregators.

### 7. Scalability Considerations

- **Heroku scaling:** Horizontal scaling via multiple Eco dynos behind Heroku's router; stateless backend (session state in Redis, not in-memory)
- **Connection pooling:** HikariCP for PostgreSQL (max pool size tuned to Heroku's connection limit — typically 20 for Hobby, 120 for Standard)
- **Redis caching strategy:** Cache generated questions with TTL (1 hour), cache user session/JWT blacklist, cache topic metadata
- **Claude API resilience:** Resilience4j circuit breaker (open after 5 consecutive failures, half-open after 30s), retry with exponential backoff (max 3 attempts), timeout at 30 seconds
- **Async processing:** Use `@Async` with a custom `ThreadPoolTaskExecutor` for non-blocking evaluation calls; or Spring WebFlux `Mono/Flux` if going reactive
- **Database optimization:** Use `@QueryHints` for read-only queries, enable Hibernate second-level cache (via Redis) for static reference data (topics, difficulty configs)

### 8. Deployment & CI/CD

#### Frontend (GitHub Pages)
- GitHub Actions workflow: on push to `main` → `npm run build` → deploy `dist/` to `gh-pages` branch
- Environment variable for API base URL injected at build time (`VITE_API_BASE_URL`)
- Use `HashRouter` (not `BrowserRouter`) since GitHub Pages doesn't support SPA fallback routing

#### Backend (Heroku)
- GitHub Actions workflow: on push to `main` → `./mvnw clean verify` (compile + test + package) → deploy JAR to Heroku via `heroku-deploy` action or container registry
- Heroku config vars for: `DATABASE_URL`, `REDIS_URL`, `CLAUDE_API_KEY`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`
- Heroku Procfile: `web: java -Dserver.port=$PORT -jar target/app.jar`
- Database migrations via Flyway (auto-run on startup)
- Health check endpoint: `/actuator/health` (Spring Boot Actuator)

#### Local Development Environment

Before any development begins, provide a **bootstrap setup script** (`scripts/setup-local.sh`) that:

1. **Detects the OS** (macOS / Linux / WSL) and installs missing prerequisites:
   - **Java 21** — check via `java --version`; if missing, install via SDKMAN (`sdk install java 21.0.4-tem`) or `brew install openjdk@21`
   - **Maven 3.9+** — check via `mvn --version`; if missing, use Maven Wrapper (`./mvnw`) bundled in the repo as the fallback (always prefer `./mvnw` in docs and CI)
   - **Node.js 20 LTS + npm** — check via `node --version`; if missing, install via `nvm install 20`
   - **PostgreSQL 16** — check via `psql --version`; if missing, install via `brew install postgresql@16` (macOS) or `apt install postgresql-16` (Linux), then auto-create the dev database: `createdb interviewprep_dev`
   - **Redis 7** — check via `redis-server --version`; if missing, install via `brew install redis` (macOS) or `apt install redis-server` (Linux), then start the service
   - **Docker (optional alternative)** — if Docker is detected, offer to spin up PostgreSQL + Redis via a `docker-compose.dev.yml` instead of local installs

2. **Creates local config files:**
   - Copies `application-dev.properties.template` → `application-dev.properties` (gitignored) with placeholders for `CLAUDE_API_KEY` and `JWT_SECRET`
   - Prompts the developer to paste their Claude API key interactively

3. **Validates the setup** by running:
   - `./mvnw validate` (Maven project structure check)
   - `pg_isready` (PostgreSQL connection check)
   - `redis-cli ping` (Redis connection check)
   - Prints a summary: ✅ Java 21, ✅ Maven 3.9.6, ✅ PostgreSQL running, ✅ Redis running, ✅ Config created

Also provide a `docker-compose.dev.yml`:
```yaml
services:
  postgres:
    image: postgres:16-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: interviewprep_dev
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: devpass
    volumes: [pgdata:/var/lib/postgresql/data]
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
volumes:
  pgdata:
```

#### Local Run Commands
- **Backend:** `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- **Frontend:** `cd frontend && npm run dev` (Vite dev server on `localhost:5173`, proxied to backend on `localhost:8080`)
- **Full stack (Docker):** `docker compose -f docker-compose.dev.yml up -d && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- **Run tests:** `./mvnw test` (unit) / `./mvnw verify -Pintegration` (integration tests with Testcontainers for PostgreSQL + Redis)

#### Monitoring
- Spring Boot Actuator endpoints: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- Sentry Java SDK for exception tracking with `@SentrySpan` on critical paths
- Custom Micrometer metrics: `interviews.started`, `interviews.completed`, `claude.api.latency`, `claude.api.errors`, `claude.tokens.used`
- Heroku log drain to a log aggregator (Papertrail or Logentries add-on)

### 9. Security Checklist

- HTTPS enforced (Heroku provides SSL by default)
- CORS whitelist restricted to GitHub Pages domain only
- JWT with short-lived access tokens (15 min) + refresh tokens (7 days) stored in HttpOnly cookies
- Passwords hashed with BCrypt (strength 12)
- Claude API key stored in Heroku config vars — NEVER in source code or frontend
- Input validation on all DTOs (`@NotBlank`, `@Size`, `@Valid`)
- SQL injection prevention via parameterized queries (JPA Criteria API / named parameters)
- Rate limiting on auth endpoints (login: 5/min, register: 3/min)
- OWASP headers via Spring Security: `X-Content-Type-Options`, `X-Frame-Options`, `CSP`, `HSTS`
- Dependency vulnerability scanning via OWASP `dependency-check-maven` plugin in CI (`./mvnw verify -Pdependency-check`)

### 10. Testing Strategy (Local + CI)

Provide a complete testing setup that works identically on a developer's local machine and in GitHub Actions:

- **Unit tests** (`src/test/java`) — JUnit 5 + Mockito for service/logic layers. Claude API calls mocked via `WireMock` stubs. Run with `./mvnw test`.
- **Integration tests** (`src/test/java`, tagged `@Tag("integration")`) — Use **Testcontainers** to spin up throwaway PostgreSQL 16 + Redis 7 Docker containers. Tests run real JPA queries and Redis operations against ephemeral instances. Run with `./mvnw verify -Pintegration`.
- **API tests** — `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebTestClient` or `MockMvc` for full request-response cycle testing of all REST endpoints including JWT auth flow.
- **Claude API contract tests** — Record real Claude responses as JSON fixtures (`src/test/resources/fixtures/claude/`). Unit tests verify the `ClaudeResponseParser` handles valid responses, malformed JSON, partial responses, and API errors gracefully.
- **Test profiles** — `application-test.properties` uses Testcontainers-managed URLs (auto-configured), disables Flyway migration (use `spring.flyway.enabled=false` with `@Sql` scripts per test), and uses a mock Claude API base URL pointing to WireMock.
- **CI pipeline** — GitHub Actions runs both `./mvnw test` and `./mvnw verify -Pintegration` with Docker available (`services: docker`). Test results published as GitHub Actions artifacts.
- **Coverage** — JaCoCo Maven plugin with minimum 70% line coverage enforced on service + aspect packages. Coverage report generated at `target/site/jacoco/`.

### 11. API Versioning & Modularity

- URL-based versioning: `/api/v1/*`
- Package structure:
  ```
  com.interviewprep
  ├── config/          (SecurityConfig, RedisConfig, WebClientConfig, AopConfig)
  ├── controller/      (InterviewController, AuthController, UserController)
  ├── dto/             (request/response DTOs — separate from entities)
  ├── entity/          (JPA entities)
  ├── repository/      (Spring Data JPA repos)
  ├── service/         (InterviewService, ClaudeApiClient, DifficultyEngine, EvaluationService)
  ├── aspect/          (LoggingAspect, AuditAspect, RateLimitAspect)
  ├── exception/       (custom exceptions + GlobalExceptionHandler)
  ├── mapper/          (MapStruct mappers: Entity ↔ DTO)
  ├── util/            (PromptTemplateBuilder, JsonSchemaValidator)
  └── filter/          (JwtAuthFilter, RequestLoggingFilter)
  ```
- Use MapStruct for Entity ↔ DTO mapping (compile-time, zero reflection)
- Global exception handling with `@ControllerAdvice` returning RFC 7807 `ProblemDetail`

---

## Constraints

- **Format:** Structured sections with clear headings. Use code blocks for schemas, configs, and prompts. Use tables for comparisons.
- **Style:** Concise, technical, implementation-focused. No filler theory — every section must be actionable.
- **Scope:** Cover architecture, API contracts, database schema, Claude prompt templates, AOP aspects, CI/CD pipelines, and deployment configs. Exclude generic explanations of what Spring Boot or React are.
- **Reasoning:** Think step-by-step. Justify each technology choice with one line (e.g., "Redis for rate limiting — atomic operations via Lua scripts, sub-ms latency").
- **Self-check:** Before finalizing, verify: (1) no component is single-point-of-failure without a fallback, (2) all API contracts have request + response examples, (3) Claude prompts enforce JSON-only output, (4) no secrets are exposed in frontend or logs, (5) GitHub Pages CORS + routing limitations are handled, (6) local dev setup works with zero manual config beyond running the setup script + pasting an API key, (7) `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` starts the app with no errors on a fresh clone.

---

## Deliverables Expected

1. Component architecture diagram description (reproducible in draw.io)
2. Full REST API spec (all endpoints, methods, request/response bodies, status codes)
3. PostgreSQL schema DDL (with indexes and constraints)
4. Claude system prompt + question generation prompt + evaluation prompt (copy-paste ready)
5. Spring Security config (JWT filter chain, CORS, OAuth2)
6. AOP aspect code (logging, audit, rate limiting)
7. `application.properties` config (with Spring profiles: `dev`, `staging`, `prod` activated via `spring.profiles.active`) — include the full property keys for each profile:
   - `application.properties` — shared defaults (server.port, spring.jpa.open-in-view=false, actuator endpoints, logging format)
   - `application-dev.properties` — local PostgreSQL (`localhost:5432/interviewprep_dev`), local Redis (`localhost:6379`), DEBUG logging, CORS allows `localhost:5173`, H2 console enabled as fallback
   - `application-prod.properties` — Heroku `${DATABASE_URL}` parsing, `${REDIS_URL}` parsing, INFO logging, CORS restricted to GitHub Pages domain, SSL enforcement via `server.forward-headers-strategy=native`
8. GitHub Actions CI/CD workflow files (frontend + backend)
9. Heroku deployment config (Procfile, `system.properties` with `java.runtime.version=21`)
10. Redis caching strategy (keys, TTLs, eviction policy)
11. Local setup script (`scripts/setup-local.sh`) — prerequisite detection, installation, DB/Redis bootstrap, config templating
12. `docker-compose.dev.yml` for local PostgreSQL + Redis
13. Maven POM (`pom.xml`) with all dependencies, profiles (`dev`, `integration`, `dependency-check`), and plugin configurations (spring-boot-maven-plugin, maven-surefire, maven-failsafe, flyway-maven-plugin, mapstruct-processor)
