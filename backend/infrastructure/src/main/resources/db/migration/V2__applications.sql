CREATE TABLE applications (
    id              UUID PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    description     VARCHAR(1000) NOT NULL DEFAULT '',
    repository_url  VARCHAR(500) NOT NULL,
    branch          VARCHAR(200) NOT NULL,
    compose_path    VARCHAR(500) NOT NULL,
    domain          VARCHAR(255) NOT NULL DEFAULT '',
    status          VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_applications_name UNIQUE (name)
);

CREATE INDEX idx_applications_name ON applications (name);
CREATE INDEX idx_applications_status ON applications (status);
CREATE INDEX idx_applications_created_at ON applications (created_at);
