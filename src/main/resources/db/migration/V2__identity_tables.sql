CREATE TABLE identity.users
(
    id           uuid PRIMARY KEY      DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL UNIQUE,
    username     VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    avatar_url   VARCHAR(500),
    language     VARCHAR(10)  NOT NULL DEFAULT 'vi',
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE identity.roles
(
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);
-- Stable reference IDs: roles are immutable reference data.
-- Hardcoding UUIDs keeps test fixtures, seed scripts, and debug queries deterministic across environments.
INSERT INTO identity.roles (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'ROLE_USER'),
       ('00000000-0000-0000-0000-000000000002', 'ROLE_ADMIN');

CREATE TABLE identity.user_roles
(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX idx_user_roles_user ON identity.user_roles (user_id);
CREATE INDEX idx_user_roles_role ON identity.user_roles (role_id);

CREATE TABLE identity.body_stats
(
    id             UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL,
    height_cm      NUMERIC(5, 2),
    weight_kg      NUMERIC(5, 2),
    date_of_birth  DATE,
    gender         VARCHAR(20),
    activity_level VARCHAR(30),
    recorded_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_body_stats_user ON identity.body_stats (user_id);

CREATE TABLE identity.refresh_tokens
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON identity.refresh_tokens (user_id);