# AI Interview Prep Platform

## Project Overview
AI-powered interview preparation platform simulating real-world technical interviews.
Full architecture spec: @docs/architecture-spec.md

## Stack
- **Backend:** Java 21, Spring Boot 3.x, Spring Security 6, Spring AOP, Spring Data JPA
- **Frontend:** React 18 (Vite), TypeScript, Tailwind CSS
- **Database:** PostgreSQL 16 (Flyway migrations)
- **Cache:** Redis 7 (Spring Data Redis)
- **AI:** Anthropic Claude API (claude-sonnet-4-20250514) via Spring RestClient
- **Build:** Maven 3.9+ (always use `./mvnw`, never bare `mvn`)
- **Hosting:** GitHub Pages (frontend), Heroku (backend)

## Commands
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` — run backend locally
- `./mvnw test` — unit tests
- `./mvnw verify -Pintegration` — integration tests (requires Docker for Testcontainers)
- `cd frontend && npm run dev` — Vite dev server on localhost:5173
- `docker compose -f docker-compose.dev.yml up -d` — local PostgreSQL + Redis

## Package Structure
```
com.interviewprep
├── config/        # SecurityConfig, RedisConfig, WebClientConfig, AopConfig
├── controller/    # REST controllers (InterviewController, AuthController)
├── dto/           # Request/Response DTOs — NEVER expose JPA entities in APIs
├── entity/        # JPA entities (User, Interview, Question, Answer)
├── repository/    # Spring Data JPA repositories
├── service/       # Business logic (InterviewService, ClaudeApiClient, DifficultyEngine)
├── aspect/        # AOP aspects (LoggingAspect, AuditAspect, RateLimitAspect)
├── exception/     # Custom exceptions + GlobalExceptionHandler (@ControllerAdvice)
├── mapper/        # MapStruct mappers (Entity ↔ DTO, compile-time)
├── util/          # PromptTemplateBuilder, JsonSchemaValidator
└── filter/        # JwtAuthFilter, RequestLoggingFilter
```

## Code Style
- Java 21 — use records for DTOs, sealed interfaces where appropriate
- NEVER use `@Autowired` on fields — use constructor injection only
- All REST endpoints under `/api/v1/`
- All properties in `application.properties` (NOT yml)
- Spring profiles: `dev`, `test`, `prod`
- Structured JSON logging via Logback + Logstash encoder
- Every service method must have `@LogExecutionTime` aspect applied
- Exception handling via `@ControllerAdvice` returning RFC 7807 ProblemDetail
- Use `jakarta.validation` annotations on all DTOs (`@NotBlank`, `@Size`, `@Valid`)

## Database
- Flyway for all migrations — NEVER use `spring.jpa.hibernate.ddl-auto=update` in prod
- Migration files: `src/main/resources/db/migration/V{number}__{description}.sql`
- Use UUID primary keys for all entities
- Store flexible data as JSONB columns (question payloads, evaluation results)

## Security Rules
- NEVER commit API keys, JWT secrets, or passwords to source code
- Claude API key comes from env var `CLAUDE_API_KEY`
- JWT secret from env var `JWT_SECRET`
- CORS restricted to frontend origin only
- Passwords hashed with BCrypt (strength 12)

## Testing
- Unit tests: JUnit 5 + Mockito, mock Claude API with WireMock
- Integration tests: Testcontainers (PostgreSQL + Redis), tagged `@Tag("integration")`
- API tests: `@SpringBootTest` + MockMvc with JWT test tokens
- Minimum 70% line coverage on service + aspect packages (JaCoCo)

## Important
- Always run `./mvnw verify` before committing
- Frontend and backend are separate deployable units
- Frontend communicates with backend via REST — use `HashRouter` (not BrowserRouter) for GitHub Pages
- When generating Claude API prompts, ALWAYS enforce JSON-only output in the system prompt
