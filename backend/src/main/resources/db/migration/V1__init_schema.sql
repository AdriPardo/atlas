CREATE TABLE users (
    id UUID PRIMARY KEY,
    installation_id UUID NOT NULL,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE hosts (
    id UUID PRIMARY KEY,
    installation_id UUID NOT NULL,
    hostname VARCHAR(255) NOT NULL,
    ip VARCHAR(64) NOT NULL,
    operating_system VARCHAR(120),
    docker_version VARCHAR(80),
    online BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE applications (
    id UUID PRIMARY KEY,
    installation_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    repository_url VARCHAR(500) NOT NULL,
    branch VARCHAR(120) NOT NULL,
    compose_path VARCHAR(255) NOT NULL,
    domain VARCHAR(255),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_applications_installation_name
    ON applications (installation_id, LOWER(name));

CREATE TABLE deployments (
    id UUID PRIMARY KEY,
    installation_id UUID NOT NULL,
    application_id UUID NOT NULL REFERENCES applications (id),
    host_id UUID NOT NULL REFERENCES hosts (id),
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    logs TEXT
);

CREATE INDEX idx_applications_installation ON applications (installation_id);
CREATE INDEX idx_hosts_installation ON hosts (installation_id);
CREATE INDEX idx_deployments_installation ON deployments (installation_id);
