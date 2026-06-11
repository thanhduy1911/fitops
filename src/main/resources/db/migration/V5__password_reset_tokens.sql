-- FO-0048
CREATE TABLE identity.password_reset_tokens
(
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- At most one live (unconsumed) reset token per user. The application also enforces this via
-- consumeAllActiveByUserId before issuing; this partial unique index is the DB-level guarantee.
CREATE UNIQUE INDEX idx_password_reset_tokens_user_unconsumed_unique
    ON identity.password_reset_tokens (user_id)
    WHERE consumed_at IS NULL;