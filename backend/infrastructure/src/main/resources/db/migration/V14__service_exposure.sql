-- Autopilot placement: service exposure (PUBLIC edge vs INTERNAL-only)

ALTER TABLE services
    ADD COLUMN exposure VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

CREATE INDEX idx_services_exposure ON services (exposure);
