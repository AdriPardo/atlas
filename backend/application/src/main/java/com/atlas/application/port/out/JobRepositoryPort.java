package com.atlas.application.port.out;

import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobStatus;
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
}
