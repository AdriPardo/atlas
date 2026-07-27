-- Product cron schedules (enqueue existing job types)
CREATE TABLE cron_jobs (
    id               UUID PRIMARY KEY,
    name             VARCHAR(128) NOT NULL,
    cron_expression  VARCHAR(128) NOT NULL,
    target_type      VARCHAR(32)  NOT NULL,
    target_id        UUID,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    last_fired_at    TIMESTAMPTZ,
    last_error       VARCHAR(512),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_cron_jobs_name UNIQUE (name),
    CONSTRAINT chk_cron_jobs_target_type CHECK (target_type IN ('SYNC_HOST', 'BACKUP_DATABASE'))
);

CREATE INDEX idx_cron_jobs_enabled ON cron_jobs (enabled);
CREATE INDEX idx_cron_jobs_target_type ON cron_jobs (target_type);
