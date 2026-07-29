-- Pipelines may omit a fixed host; RunPipeline uses Autopilot placement when null.
ALTER TABLE pipelines
    ALTER COLUMN host_id DROP NOT NULL;
