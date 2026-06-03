-- [FO-0045] case-insensitive username uniqueness (display case preserved)
ALTER TABLE identity.users
    DROP CONSTRAINT users_username_key;
CREATE UNIQUE INDEX uq_users_username_lower ON identity.users (LOWER(username));