package com.atlas.domain.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JobTest {

    @Test
    void markFailedClearsLease() {
        Job job = Job.enqueue(JobType.SYNC_HOST, "{}", 3);
        job.markRunning("worker-a", 1);
        assertEquals(JobStatus.RUNNING, job.getStatus());
        assertEquals("worker-a", job.getLockedBy());
        assertNotNull(job.getLockedAt());

        job.markFailed("boom");

        assertEquals(JobStatus.FAILED, job.getStatus());
        assertNull(job.getLockedBy());
        assertNull(job.getLockedAt());
        assertEquals("boom", job.getLastError());
    }

    @Test
    void touchLockRefreshesLockedAt() {
        Job job = Job.enqueue(JobType.SYNC_HOST, "{}", 3);
        job.markRunning("worker-a", 1);
        Instant first = job.getLockedAt();

        job.touchLock();

        assertTrue(!job.getLockedAt().isBefore(first));
        assertEquals("worker-a", job.getLockedBy());
        assertEquals(JobStatus.RUNNING, job.getStatus());
    }

    @Test
    void touchLockRejectsNonRunning() {
        Job job = Job.enqueue(JobType.SYNC_HOST, "{}", 3);
        assertThrows(DomainException.class, job::touchLock);
    }
}
