-- Initial auth schema: users + profiles (1:1) + refresh_tokens.
-- UUID primary keys are stored as BINARY(16). `password` and `token` hold Argon2 hashes.

CREATE TABLE users (
    id                 BINARY(16)    NOT NULL,
    email              VARCHAR(255)  NOT NULL,
    password           VARCHAR(255)  NULL,             -- NULL for OAuth-only accounts
    role               VARCHAR(32)   NOT NULL DEFAULT 'USER',
    google_sub         VARCHAR(255)  NULL,             -- Google OpenID "sub", NULL until linked
    two_factor_enabled BIT(1)        NOT NULL DEFAULT b'0',
    two_factor_secret  VARCHAR(255)  NULL,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_google_sub UNIQUE (google_sub)
) ENGINE = InnoDB;

CREATE TABLE profiles (
    user_id       BINARY(16)    NOT NULL,
    first_name    VARCHAR(100)  NULL,
    last_name     VARCHAR(100)  NULL,
    avatar_url    VARCHAR(512)  NULL,
    phone         VARCHAR(30)   NULL,
    bio           VARCHAR(1000) NULL,
    -- Address (embedded)
    street_number VARCHAR(20)   NULL,
    street_name   VARCHAR(255)  NULL,
    address_line2 VARCHAR(255)  NULL,
    city          VARCHAR(120)  NULL,
    postal_code   VARCHAR(20)   NULL,
    country       VARCHAR(100)  NULL,
    latitude      DECIMAL(9, 6) NULL,
    longitude     DECIMAL(9, 6) NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE refresh_tokens (
    id         BINARY(16)   NOT NULL,
    user_id    BINARY(16)   NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    revoked    BIT(1)       NOT NULL DEFAULT b'0',
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
