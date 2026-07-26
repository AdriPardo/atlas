package com.atlas;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlas.application.job.ClaimJobsUseCase;
import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobStatus;
import com.atlas.domain.job.JobType;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
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
class JobClaimIntegrationTest {

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

    @Test
    void claimUsesSkipLockedAndDoesNotDuplicateAcrossWorkers() throws Exception {
        for (int i = 0; i < 10; i++) {
            enqueueJobUseCase.execute(new EnqueueJobUseCase.EnqueueJobCommand(
                    JobType.SYNC_HOST, "{\"hostId\":\"" + java.util.UUID.randomUUID() + "\"}", 3));
        }

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<List<Job>> w1 = pool.submit(() -> {
            start.await(5, TimeUnit.SECONDS);
            return claimJobsUseCase.execute("worker-a", 5);
        });
        Future<List<Job>> w2 = pool.submit(() -> {
            start.await(5, TimeUnit.SECONDS);
            return claimJobsUseCase.execute("worker-b", 5);
        });
        start.countDown();

        List<Job> a = w1.get(20, TimeUnit.SECONDS);
        List<Job> b = w2.get(20, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(a).hasSize(5);
        assertThat(b).hasSize(5);
        assertThat(Stream.concat(a.stream(), b.stream()).map(Job::getId).distinct().count()).isEqualTo(10);
        assertThat(Stream.concat(a.stream(), b.stream())).allMatch(job -> job.getStatus() == JobStatus.RUNNING);
        assertThat(a).allMatch(job -> "worker-a".equals(job.getLockedBy()));
        assertThat(b).allMatch(job -> "worker-b".equals(job.getLockedBy()));
    }
}
