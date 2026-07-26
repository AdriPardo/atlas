-- Organization (single-tenant seed) + Project/Service split from Application (ADR-0004)

CREATE TABLE organizations (
    id          UUID PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(150) NOT NULL,
    settings    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

INSERT INTO organizations (id, name, slug, settings, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Atlas',
    'atlas',
    '{}'::jsonb,
    NOW()
);

CREATE TABLE projects (
    id               UUID PRIMARY KEY,
    organization_id  UUID         NOT NULL,
    name             VARCHAR(150) NOT NULL,
    slug             VARCHAR(150) NOT NULL,
    description      VARCHAR(1000) NOT NULL DEFAULT '',
    status           VARCHAR(50)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_projects_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
    CONSTRAINT uq_projects_org_slug UNIQUE (organization_id, slug),
    CONSTRAINT uq_projects_org_name UNIQUE (organization_id, name)
);

CREATE INDEX idx_projects_organization_id ON projects (organization_id);
CREATE INDEX idx_projects_status ON projects (status);
CREATE INDEX idx_projects_created_at ON projects (created_at);

CREATE TABLE services (
    id               UUID PRIMARY KEY,
    project_id       UUID         NOT NULL,
    name             VARCHAR(150) NOT NULL,
    repository_url   VARCHAR(500) NOT NULL,
    branch           VARCHAR(200) NOT NULL,
    compose_path     VARCHAR(500) NOT NULL,
    domain           VARCHAR(255) NOT NULL DEFAULT '',
    environment      VARCHAR(50)  NOT NULL DEFAULT 'default',
    status           VARCHAR(50)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_services_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT uq_services_project_name UNIQUE (project_id, name)
);

CREATE INDEX idx_services_project_id ON services (project_id);
CREATE INDEX idx_services_status ON services (status);
CREATE INDEX idx_services_created_at ON services (created_at);

-- Each Application → 1 Project (same id) + 1 default Service (new id)
INSERT INTO projects (id, organization_id, name, slug, description, status, created_at, updated_at)
SELECT
    a.id,
    '00000000-0000-0000-0000-000000000001',
    a.name,
    lower(regexp_replace(regexp_replace(a.name, '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g')),
    a.description,
    a.status,
    a.created_at,
    a.updated_at
FROM applications a;

INSERT INTO services (
    id, project_id, name, repository_url, branch, compose_path, domain, environment, status, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    a.id,
    'default',
    a.repository_url,
    a.branch,
    a.compose_path,
    a.domain,
    'default',
    a.status,
    a.created_at,
    a.updated_at
FROM applications a;

-- Remap deployments to services
ALTER TABLE deployments ADD COLUMN service_id UUID;

UPDATE deployments d
SET service_id = s.id
FROM services s
WHERE s.project_id = d.application_id;

ALTER TABLE deployments
    ALTER COLUMN service_id SET NOT NULL;

ALTER TABLE deployments DROP CONSTRAINT fk_deployments_application;
DROP INDEX IF EXISTS idx_deployments_application_id;
ALTER TABLE deployments DROP COLUMN application_id;

ALTER TABLE deployments
    ADD CONSTRAINT fk_deployments_service
        FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE RESTRICT;

CREATE INDEX idx_deployments_service_id ON deployments (service_id);

DROP TABLE applications;
