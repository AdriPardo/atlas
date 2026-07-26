CREATE TABLE secrets (
    id           UUID PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    ciphertext   TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_secrets_name UNIQUE (name)
);

CREATE INDEX idx_secrets_created_at ON secrets (created_at);
