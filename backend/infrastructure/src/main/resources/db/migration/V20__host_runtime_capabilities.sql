-- Persist Host runtime capability tags (ADR-0014). Default compose for existing rows.
ALTER TABLE hosts
    ADD COLUMN runtime_capabilities JSONB NOT NULL DEFAULT '["compose"]'::jsonb;
