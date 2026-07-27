-- Domains + certificate metadata (networking control plane)
CREATE TABLE domains (
    id                       UUID PRIMARY KEY,
    project_id               UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    service_id               UUID         REFERENCES services (id) ON DELETE SET NULL,
    hostname                 VARCHAR(253) NOT NULL,
    status                   VARCHAR(32)  NOT NULL,
    verification_token       VARCHAR(128) NOT NULL,
    certificate_issuer       VARCHAR(128),
    certificate_expires_at   TIMESTAMPTZ,
    certificate_sans         VARCHAR(512),
    verified_at              TIMESTAMPTZ,
    last_error               VARCHAR(512),
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_domains_project_hostname UNIQUE (project_id, hostname),
    CONSTRAINT chk_domains_status CHECK (status IN ('PENDING_DNS', 'ACTIVE', 'ERROR'))
);

CREATE INDEX idx_domains_project_id ON domains (project_id);
CREATE INDEX idx_domains_service_id ON domains (service_id);
CREATE INDEX idx_domains_status ON domains (status);
