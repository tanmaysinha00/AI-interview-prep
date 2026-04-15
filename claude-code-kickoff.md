# Claude Code Kickoff Prompt
# ===========================
# Paste this as your FIRST message to Claude Code after setting up the project.
# This tells Claude Code to read the spec and start scaffolding.


## Prompt to paste:

---

Read the full architecture spec at @docs/architecture-spec.md — this is the complete system design for the project.

Start by scaffolding the project in this order:

1. **Prerequisites check** — create `scripts/setup-local.sh` that detects OS, checks for Java 21, Maven, Node 20, PostgreSQL 16, Redis 7, Docker, and installs anything missing. Include validation at the end (pg_isready, redis-cli ping, ./mvnw validate).

2. **Maven project structure** — generate the `pom.xml` with all dependencies from the spec (Spring Boot 3.x starter-web, starter-data-jpa, starter-security, starter-data-redis, starter-aop, starter-actuator, starter-validation, springdoc-openapi, flyway-core, mapstruct, resilience4j, jackson, jjwt, micrometer-prometheus, logstash-logback-encoder, sentry-spring-boot, testcontainers, wiremock, jacoco). Include Maven profiles for `dev`, `integration`, and `dependency-check`.

3. **Application properties** — create `application.properties`, `application-dev.properties`, `application-test.properties`, and `application-dev.properties.template` per the spec.

4. **Docker Compose** — create `docker-compose.dev.yml` for local PostgreSQL 16 + Redis 7.

5. **Package skeleton** — create empty packages matching the structure in CLAUDE.md (config, controller, dto, entity, repository, service, aspect, exception, mapper, util, filter).

6. **Flyway baseline migration** — create `V1__baseline.sql` with the full PostgreSQL schema from the spec (users, interviews, questions, answers, token_usage tables with UUID PKs, JSONB columns, indexes).

Do NOT build the full application yet. Just scaffold the project structure, dependencies, configs, and database schema so I can verify the foundation before we build features.

---
