package com.atlas.application.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.observability.EvaluateProductAlertsUseCase;
import com.atlas.application.port.out.BillingMeterPort;
import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.domain.billing.UsageMeters;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobStatus;
import com.atlas.domain.job.JobType;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.shared.NotFoundException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobMinutesMeteringUseCasesTest {

    @Mock
    private JobRepositoryPort jobRepository;

    @Mock
    private BillingMeterPort billingMeter;

    @Mock
    private EvaluateProductAlertsUseCase evaluateProductAlertsUseCase;

    @InjectMocks
    private CompleteJobUseCase completeJobUseCase;

    @InjectMocks
    private FailJobUseCase failJobUseCase;

    @Test
    void completeRecordsJobMinutes() {
        Instant started = Instant.now().minus(Duration.ofMinutes(2));
        Job running = rehydrateRunning(started);
        when(jobRepository.findById(running.getId())).thenReturn(Optional.of(running));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Job saved = completeJobUseCase.execute(running.getId());

        assertEquals(JobStatus.SUCCEEDED, saved.getStatus());
        ArgumentCaptor<BigDecimal> qty = ArgumentCaptor.forClass(BigDecimal.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> dims = ArgumentCaptor.forClass(Map.class);
        verify(billingMeter).record(eq(UsageMeters.JOB_MINUTES), qty.capture(), dims.capture());
        assertTrue(qty.getValue().compareTo(new BigDecimal("1.99")) >= 0);
        assertTrue(qty.getValue().compareTo(new BigDecimal("2.05")) <= 0);
        assertEquals(JobType.SYNC_HOST.name(), dims.getValue().get("jobType"));
        assertEquals(JobStatus.SUCCEEDED.name(), dims.getValue().get("status"));
    }

    @Test
    void failRecordsJobMinutes() {
        Instant started = Instant.now().minus(Duration.ofSeconds(90));
        Job running = rehydrateRunning(started);
        when(jobRepository.findById(running.getId())).thenReturn(Optional.of(running));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Job saved = failJobUseCase.execute(running.getId(), "boom");

        assertEquals(JobStatus.FAILED, saved.getStatus());
        ArgumentCaptor<BigDecimal> qty = ArgumentCaptor.forClass(BigDecimal.class);
        verify(billingMeter).record(eq(UsageMeters.JOB_MINUTES), qty.capture(), any());
        assertTrue(qty.getValue().compareTo(new BigDecimal("1.49")) >= 0);
        assertTrue(qty.getValue().compareTo(new BigDecimal("1.55")) <= 0);
        verify(evaluateProductAlertsUseCase)
                .execute(eq(AlertEventType.JOB_FAILED), any(), any(), eq("job"), eq(saved.getId()));
    }

    @Test
    void completeSkipsMeterWithoutStartedAt() {
        Job pending = Job.enqueue(JobType.SYNC_HOST, "{}", 3);
        when(jobRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        completeJobUseCase.execute(pending.getId());

        verify(billingMeter, never()).record(any(), any(), any());
    }

    @Test
    void completeNotFound() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> completeJobUseCase.execute(id));
        verify(billingMeter, never()).record(any(), any(), any());
    }

    private static Job rehydrateRunning(Instant startedAt) {
        return Job.rehydrate(
                UUID.randomUUID(),
                JobType.SYNC_HOST,
                "{\"hostId\":\"" + UUID.randomUUID() + "\"}",
                JobStatus.RUNNING,
                1,
                3,
                startedAt,
                startedAt,
                "worker-a",
                startedAt,
                null,
                null,
                startedAt,
                startedAt);
    }
}
