-- ============================================================
-- V1__baseline.sql — Full schema baseline
-- Uses VARCHAR + CHECK constraints (better ORM compatibility
-- than PostgreSQL ENUM types in prepared statements).
-- ============================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- users
-- ============================================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    provider      VARCHAR(20)  NOT NULL DEFAULT 'LOCAL'
                      CHECK (provider IN ('LOCAL', 'GOOGLE', 'GITHUB')),
    provider_id   VARCHAR(255),
    display_name  VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login    TIMESTAMPTZ,

    CONSTRAINT users_email_provider_uq UNIQUE (email, provider)
);

CREATE INDEX idx_users_email ON users (email);

-- ============================================================
-- interviews
-- ============================================================
CREATE TABLE interviews (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL DEFAULT 'CREATED'
                     CHECK (status IN ('CREATED', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    topics       JSONB NOT NULL,
    config       JSONB NOT NULL,
    total_score  NUMERIC(5, 2),
    started_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_interviews_user_id ON interviews (user_id);
CREATE INDEX idx_interviews_status  ON interviews (status);

-- ============================================================
-- questions
-- ============================================================
CREATE TABLE questions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id     UUID NOT NULL REFERENCES interviews (id) ON DELETE CASCADE,
    sequence_number  INT NOT NULL,
    topic            VARCHAR(100) NOT NULL,
    difficulty       VARCHAR(10) NOT NULL
                         CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD', 'EXPERT')),
    type             VARCHAR(30) NOT NULL
                         CHECK (type IN ('MCQ', 'SHORT_ANSWER', 'SCENARIO',
                                         'CODE_REVIEW', 'HANDS_ON_CODING', 'SYSTEM_DESIGN')),
    question_payload JSONB NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT questions_interview_seq_uq UNIQUE (interview_id, sequence_number)
);

CREATE INDEX idx_questions_interview_id ON questions (interview_id);

-- ============================================================
-- answers
-- ============================================================
CREATE TABLE answers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id         UUID NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    user_answer         TEXT NOT NULL,
    evaluation_payload  JSONB,
    score               NUMERIC(4, 2),
    time_taken_seconds  INT,
    answered_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT answers_question_id_uq UNIQUE (question_id)
);

CREATE INDEX idx_answers_question_id ON answers (question_id);

-- ============================================================
-- token_usage
-- ============================================================
CREATE TABLE token_usage (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id  UUID NOT NULL REFERENCES interviews (id) ON DELETE CASCADE,
    input_tokens  INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    model         VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_token_usage_interview_id ON token_usage (interview_id);

-- ============================================================
-- View: aggregate token budget per interview
-- ============================================================
CREATE VIEW interview_token_totals AS
SELECT
    interview_id,
    SUM(input_tokens)               AS total_input_tokens,
    SUM(output_tokens)              AS total_output_tokens,
    SUM(input_tokens + output_tokens) AS total_tokens
FROM token_usage
GROUP BY interview_id;
