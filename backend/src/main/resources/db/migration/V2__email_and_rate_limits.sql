-- E-mail address per account (verified before the first login), one-time e-mail links,
-- rate-limit state (Bucket4j) and used ALTCHA challenges.

ALTER TABLE app_user
    ADD COLUMN email             VARCHAR(254),
    ADD COLUMN email_verified_at TIMESTAMPTZ;

-- accounts created before e-mail addresses existed (local development only) keep working
UPDATE app_user SET email_verified_at = created_at;

ALTER TABLE app_user
    ADD CONSTRAINT app_user_email_uk UNIQUE (email),
    ADD CONSTRAINT app_user_email_lower_ck CHECK (email = lower(email));

-- One-time links sent by e-mail. Only the SHA-256 of the token is stored; the token itself is only in the e-mail.
CREATE TABLE email_token (
    token_hash CHAR(64)    PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    purpose    VARCHAR(10) NOT NULL CHECK (purpose IN ('VERIFY', 'RESET')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX email_token_user_idx ON email_token (user_id, purpose);

-- Bucket4j rate-limit buckets. id = SHA-256 of "<limit>:<key>", so IPs and e-mail addresses are not stored in clear.
-- expires_at: epoch millis after which the bucket is full again and can be deleted.
CREATE TABLE rate_limit_bucket (
    id         VARCHAR(64) PRIMARY KEY,
    state      BYTEA,
    expires_at BIGINT
);

-- ALTCHA challenges that were already used for a registration (each solution counts once).
CREATE TABLE altcha_used (
    signature  VARCHAR(128) PRIMARY KEY,
    expires_at TIMESTAMPTZ  NOT NULL
);
