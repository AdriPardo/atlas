CREATE TABLE hosts (
    id                UUID PRIMARY KEY,
    hostname          VARCHAR(255) NOT NULL,
    ip                VARCHAR(64)  NOT NULL,
    operating_system  VARCHAR(150) NOT NULL,
    docker_version    VARCHAR(100) NOT NULL DEFAULT '',
    online            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_hosts_hostname UNIQUE (hostname)
);

CREATE INDEX idx_hosts_hostname ON hosts (hostname);
CREATE INDEX idx_hosts_online ON hosts (online);
CREATE INDEX idx_hosts_created_at ON hosts (created_at);
