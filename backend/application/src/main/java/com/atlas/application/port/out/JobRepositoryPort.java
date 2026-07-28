package com.atlas.application.port.out;

import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepositoryPort {

    Job save(Job job);

    Optional<Job> findById(UUID id);

    PageResult<Job> search(JobStatus status, PageQuery pageQuery);

    /**
     * Atomically claims up to {@code limit} PENDING jobs using SKIP LOCKED.
     */
    List<Job> claimPending(String workerId, int limit);

    /**
     * Locks up to {@code limit} RUNNING jobs whose lease ({@code locked_at}) is older than {@code
     * cutoff}, using SKIP LOCKED so healthy workers holding the row are not disturbed.
     */
    List<Job> findAndLockStaleRunning(Instant cutoff, int limit);

    /**
     * Refreshes {@code locked_at} for a job still RUNNING under {@code workerId}. Returns {@code
     * true} if a row was updated.
     */
    boolean heartbeat(UUID jobId, String workerId);

    /** Sets {@code locked_at} for a job (tests / ops: simulate an aged lease after worker crash). */
    void updateLockedAt(UUID jobId, Instant lockedAt);

    /** Deletes terminal jobs (SUCCEEDED/FAILED/CANCELLED) created before cutoff. */
    int deleteTerminalOlderThan(Instant cutoff);
}
