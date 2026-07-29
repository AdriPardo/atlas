-- ADR-0014 phase C: compose_path optional when atlas.yml supplies runtime.composeFile
ALTER TABLE services
    ALTER COLUMN compose_path DROP NOT NULL;
