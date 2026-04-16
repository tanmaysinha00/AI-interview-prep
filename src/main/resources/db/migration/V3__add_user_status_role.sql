-- Add role, status, login tracking to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(10) NOT NULL DEFAULT 'USER'
    CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(10) NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED'));

ALTER TABLE users ADD COLUMN IF NOT EXISTS login_count INT NOT NULL DEFAULT 0;

-- last_login already exists — skip if present
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='users' AND column_name='last_login'
    ) THEN
        ALTER TABLE users ADD COLUMN last_login TIMESTAMPTZ;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);
CREATE INDEX IF NOT EXISTS idx_users_role   ON users (role);
