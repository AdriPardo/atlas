-- v0.9 performance: accelerate project name search (ILIKE / lower(name) LIKE)
CREATE INDEX IF NOT EXISTS idx_projects_name_lower ON projects (lower(name));
