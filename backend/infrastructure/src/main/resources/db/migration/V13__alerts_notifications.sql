-- Alerts + notification channels (product observability)
CREATE TABLE notification_channels (
    id          UUID PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    target      VARCHAR(512) NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_notification_channels_name UNIQUE (name),
    CONSTRAINT chk_notification_channels_type CHECK (type IN ('WEBHOOK', 'EMAIL'))
);

CREATE TABLE alert_rules (
    id             UUID PRIMARY KEY,
    name           VARCHAR(128) NOT NULL,
    event_type     VARCHAR(32)  NOT NULL,
    project_id     UUID         REFERENCES projects (id) ON DELETE CASCADE,
    channel_id     UUID         NOT NULL REFERENCES notification_channels (id) ON DELETE RESTRICT,
    status         VARCHAR(32)  NOT NULL,
    last_fired_at  TIMESTAMPTZ,
    last_error     VARCHAR(512),
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_alert_rules_event_type CHECK (event_type IN ('DEPLOY_FAILED', 'JOB_FAILED')),
    CONSTRAINT chk_alert_rules_status CHECK (status IN ('OK', 'PENDING', 'FIRING', 'SILENCED'))
);

CREATE INDEX idx_alert_rules_event_type ON alert_rules (event_type);
CREATE INDEX idx_alert_rules_project_id ON alert_rules (project_id);
CREATE INDEX idx_alert_rules_channel_id ON alert_rules (channel_id);
CREATE INDEX idx_alert_rules_status ON alert_rules (status);
