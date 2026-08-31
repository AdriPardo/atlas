-- Optional post-deploy migration config per service (platform override of atlas.yml runtime.migration)

ALTER TABLE services ADD COLUMN migration_enabled BOOLEAN;
ALTER TABLE services ADD COLUMN migration_strategy VARCHAR(30);
ALTER TABLE services ADD COLUMN migration_command VARCHAR(500);
ALTER TABLE services ADD COLUMN migration_container VARCHAR(100);
