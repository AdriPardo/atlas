-- Project-scoped secrets + optional bindings of org/global secrets into a project.
-- Existing rows remain global (project_id NULL).

ALTER TABLE secrets DROP CONSTRAINT IF EXISTS uq_secrets_name;

ALTER TABLE secrets
    ADD COLUMN project_id UUID REFERENCES projects (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uq_secrets_global_name_ci
    ON secrets (LOWER(name))
    WHERE project_id IS NULL;

CREATE UNIQUE INDEX uq_secrets_project_name_ci
    ON secrets (project_id, LOWER(name))
    WHERE project_id IS NOT NULL;

CREATE INDEX idx_secrets_project_id ON secrets (project_id);

CREATE TABLE project_secret_bindings (
    id         UUID PRIMARY KEY,
    project_id UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    secret_id  UUID         NOT NULL REFERENCES secrets (id) ON DELETE CASCADE,
    alias      VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_project_secret_bindings_secret UNIQUE (project_id, secret_id)
);

CREATE INDEX idx_project_secret_bindings_project_id ON project_secret_bindings (project_id);
CREATE INDEX idx_project_secret_bindings_secret_id ON project_secret_bindings (secret_id);
CREATE UNIQUE INDEX uq_project_secret_bindings_alias_ci
    ON project_secret_bindings (project_id, LOWER(alias));
