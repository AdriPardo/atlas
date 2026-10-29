CREATE TABLE api_keys (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL,
    name         VARCHAR(80)  NOT NULL,
    key_prefix   VARCHAR(16)  NOT NULL,
    key_hash     VARCHAR(64)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    last_used_at TIMESTAMPTZ,
    revoked_at   TIMESTAMPTZ,
    CONSTRAINT fk_api_keys_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_api_keys_hash UNIQUE (key_hash)
);

CREATE UNIQUE INDEX uq_api_keys_user_name_active
    ON api_keys (user_id, name)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_api_keys_user_id ON api_keys (user_id);
