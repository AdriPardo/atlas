CREATE TABLE pipelines (
    id              UUID PRIMARY KEY,
    project_id      UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name            VARCHAR(150) NOT NULL,
    service_id      UUID         NOT NULL REFERENCES services (id),
    host_id         UUID         NOT NULL REFERENCES hosts (id),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_pipelines_project_name UNIQUE (project_id, name)
);

CREATE INDEX idx_pipelines_project_id ON pipelines (project_id);
CREATE INDEX idx_pipelines_service_id ON pipelines (service_id);
CREATE INDEX idx_pipelines_created_at ON pipelines (created_at);

CREATE TABLE pipeline_runs (
    id              UUID PRIMARY KEY,
    pipeline_id     UUID         NOT NULL REFERENCES pipelines (id) ON DELETE CASCADE,
    status          VARCHAR(32)  NOT NULL,
    triggered_by    VARCHAR(128) NOT NULL DEFAULT 'manual',
    deployment_id   UUID         REFERENCES deployments (id),
    job_id          UUID         REFERENCES jobs (id),
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_pipeline_runs_status CHECK (
        status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')
    )
);

CREATE INDEX idx_pipeline_runs_pipeline_id ON pipeline_runs (pipeline_id);
CREATE INDEX idx_pipeline_runs_status ON pipeline_runs (status);
CREATE INDEX idx_pipeline_runs_created_at ON pipeline_runs (created_at);
