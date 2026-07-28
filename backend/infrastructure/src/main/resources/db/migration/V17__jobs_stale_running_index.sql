-- Speeds reclaim of orphaned RUNNING leases after worker crash (SKIP LOCKED scan by locked_at).
CREATE INDEX idx_jobs_stale_running
    ON jobs (locked_at)
    WHERE status = 'RUNNING';
