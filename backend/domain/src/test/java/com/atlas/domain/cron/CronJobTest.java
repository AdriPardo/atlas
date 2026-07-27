package com.atlas.domain.cron;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.shared.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CronJobTest {

    @Test
    void syncHostRequiresTargetId() {
        assertThrows(
                DomainException.class,
                () -> CronJob.create("sync", "0 */5 * * * *", CronTargetType.SYNC_HOST, null));
    }

    @Test
    void backupRejectsTargetId() {
        assertThrows(
                DomainException.class,
                () -> CronJob.create(
                        "backup", "0 30 2 * * *", CronTargetType.BACKUP_DATABASE, UUID.randomUUID()));
    }

    @Test
    void createAndMarkFired() {
        UUID hostId = UUID.randomUUID();
        CronJob job = CronJob.create("sync-a", "0 */10 * * * *", CronTargetType.SYNC_HOST, hostId);
        assertTrue(job.isEnabled());
        assertEquals(hostId, job.getTargetId());
        job.markFired();
        assertTrue(job.getLastFiredAt() != null);
        assertEquals(null, job.getLastError());
        job.markError("boom");
        assertFalse(job.getLastError().isBlank());
    }
}
