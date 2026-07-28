package com.atlas;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlas.application.job.ClaimJobsUseCase;
import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.application.job.RecoverStaleJobsUseCase;
import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobStatus;
import com.atlas.domain.job.JobType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class JobStaleRecoveryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("atlas")
            .withUsername("atlas")
            .withPassword("atlas");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EnqueueJobUseCase enqueueJobUseCase;

    @Autowired
    private ClaimJobsUseCase claimJobsUseCase;

    @Autowired
    private RecoverStaleJobsUseCase recoverStaleJobsUseCase;

    @Autowired
    private JobRepositoryPort jobRepository;

    @Test
    void recoversStaleRunningJobAfterWorkerCrash() {
        Job enqueued = enqueueJobUseCase.execute(new EnqueueJobUseCase.EnqueueJobCommand(
                JobType.SYNC_HOST, "{\"hostId\":\"" + UUID.randomUUID() + "\"}", 3));

        List<Job> claimed = claimJobsUseCase.execute("crashed-worker", 1);
        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).getId()).isEqualTo(enqueued.getId());
        assertThat(claimed.get(0).getStatus()).isEqualTo(JobStatus.RUNNING);

        // Simulate crash: lease age exceeds reclaim timeout (healthy SKIP LOCKED path left the row RUNNING).
        jobRepository.updateLockedAt(enqueued.getId(), Instant.now().minus(Duration.ofHours(2)));

        int recovered = recoverStaleJobsUseCase.execute(Duration.ofMinutes(30));
        assertThat(recovered).isEqualTo(1);

        Job failed = jobRepository.findById(enqueued.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(failed.getLockedBy()).isNull();
        assertThat(failed.getLockedAt()).isNull();
        assertThat(failed.getLastError()).contains(RecoverStaleJobsUseCase.STALE_ERROR_PREFIX);

        // Fresh PENDING jobs still claimable — reclaim must not break SKIP LOCKED for healthy work.
        enqueueJobUseCase.execute(new EnqueueJobUseCase.EnqueueJobCommand(
                JobType.SYNC_HOST, "{\"hostId\":\"" + UUID.randomUUID() + "\"}", 3));
        List<Job> next = claimJobsUseCase.execute("healthy-worker", 5);
        assertThat(next).hasSize(1);
        assertThat(next.get(0).getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(next.get(0).getLockedBy()).isEqualTo("healthy-worker");
    }

    @Test
    void doesNotReclaimFreshLease() {
        Job enqueued = enqueueJobUseCase.execute(new EnqueueJobUseCase.EnqueueJobCommand(
                JobType.SYNC_HOST, "{\"hostId\":\"" + UUID.randomUUID() + "\"}", 3));
        claimJobsUseCase.execute("alive-worker", 1);

        int recovered = recoverStaleJobsUseCase.execute(Duration.ofMinutes(30));
        assertThat(recovered).isZero();

        Job stillRunning = jobRepository.findById(enqueued.getId()).orElseThrow();
        assertThat(stillRunning.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(stillRunning.getLockedBy()).isEqualTo("alive-worker");
    }

    @Test
    void heartbeatRefreshesLockedAt() {
        Job enqueued = enqueueJobUseCase.execute(new EnqueueJobUseCase.EnqueueJobCommand(
                JobType.SYNC_HOST, "{\"hostId\":\"" + UUID.randomUUID() + "\"}", 3));
        claimJobsUseCase.execute("alive-worker", 1);

        Instant oldLease = Instant.now().minus(Duration.ofMinutes(10));
        jobRepository.updateLockedAt(enqueued.getId(), oldLease);

        assertThat(jobRepository.heartbeat(enqueued.getId(), "alive-worker")).isTrue();

        Job refreshed = jobRepository.findById(enqueued.getId()).orElseThrow();
        assertThat(refreshed.getLockedAt()).isAfter(oldLease);

        assertThat(jobRepository.heartbeat(enqueued.getId(), "other-worker")).isFalse();
    }
}
