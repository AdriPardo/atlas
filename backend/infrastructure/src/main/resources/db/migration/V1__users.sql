-- Atlas users
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE INDEX idx_users_username ON users (username);
