CREATE TABLE project_memberships (
    id              UUID PRIMARY KEY,
    project_id      UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role            VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_project_memberships UNIQUE (project_id, user_id),
    CONSTRAINT chk_project_membership_role CHECK (role IN ('VIEWER', 'DEVELOPER', 'OPERATOR'))
);

CREATE INDEX idx_project_memberships_user_id ON project_memberships (user_id);
CREATE INDEX idx_project_memberships_project_id ON project_memberships (project_id);

CREATE TABLE audit_entries (
    id              UUID PRIMARY KEY,
    actor_user_id   UUID,
    actor_username  VARCHAR(150) NOT NULL,
    action          VARCHAR(64)  NOT NULL,
    resource_type   VARCHAR(64)  NOT NULL,
    resource_id     UUID,
    metadata        TEXT         NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_audit_entries_created_at ON audit_entries (created_at DESC);
CREATE INDEX idx_audit_entries_actor ON audit_entries (actor_user_id);
CREATE INDEX idx_audit_entries_resource ON audit_entries (resource_type, resource_id);
