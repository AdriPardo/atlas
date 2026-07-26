CREATE TABLE deployments (
    id              UUID PRIMARY KEY,
    application_id  UUID         NOT NULL,
    host_id         UUID         NOT NULL,
    status          VARCHAR(50)  NOT NULL,
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    logs            TEXT         NOT NULL DEFAULT '',
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_deployments_application
        FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE RESTRICT,
    CONSTRAINT fk_deployments_host
        FOREIGN KEY (host_id) REFERENCES hosts (id) ON DELETE RESTRICT
);

CREATE INDEX idx_deployments_application_id ON deployments (application_id);
CREATE INDEX idx_deployments_host_id ON deployments (host_id);
CREATE INDEX idx_deployments_status ON deployments (status);
CREATE INDEX idx_deployments_created_at ON deployments (created_at);
