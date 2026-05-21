-- [FO-0040] removing DEFAULT gen_random_uuid()
ALTER TABLE identity.users
    ALTER COLUMN id DROP DEFAULT;
ALTER TABLE identity.roles
    ALTER COLUMN id DROP DEFAULT;
ALTER TABLE identity.body_stats
    ALTER COLUMN id DROP DEFAULT;
ALTER TABLE identity.refresh_tokens
    ALTER COLUMN id DROP DEFAULT;