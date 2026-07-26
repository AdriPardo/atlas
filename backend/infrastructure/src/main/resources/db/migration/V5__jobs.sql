CREATE TABLE jobs (
    id              UUID PRIMARY KEY,
    type            VARCHAR(64)  NOT NULL,
    payload         TEXT         NOT NULL DEFAULT '{}',
    status          VARCHAR(32)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    max_attempts    INT          NOT NULL DEFAULT 3,
    available_at    TIMESTAMPTZ  NOT NULL,
    locked_at       TIMESTAMPTZ,
    locked_by       VARCHAR(128),
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_jobs_attempts CHECK (attempts >= 0 AND max_attempts > 0)
);

CREATE INDEX idx_jobs_claim
    ON jobs (status, available_at, created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_type ON jobs (type);
CREATE INDEX idx_jobs_created_at ON jobs (created_at);
