ALTER TABLE pipelines
    ADD COLUMN webhook_token VARCHAR(80);

UPDATE pipelines
SET webhook_token = 'atk_migrated_' || replace(id::text, '-', '')
WHERE webhook_token IS NULL;

ALTER TABLE pipelines
    ALTER COLUMN webhook_token SET NOT NULL;

CREATE UNIQUE INDEX uq_pipelines_webhook_token ON pipelines (webhook_token);
