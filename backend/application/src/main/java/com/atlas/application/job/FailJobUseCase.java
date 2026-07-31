package com.atlas.application.job;

import com.atlas.application.observability.EvaluateProductAlertsUseCase;
import com.atlas.application.port.out.BillingMeterPort;
import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.domain.job.Job;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FailJobUseCase {

    private final JobRepositoryPort jobRepository;
    private final EvaluateProductAlertsUseCase evaluateProductAlertsUseCase;
    private final BillingMeterPort billingMeter;

    @Transactional
    public Job execute(UUID jobId, String error) {
        Job job = jobRepository
                .findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found: " + jobId));
        String message = error == null ? "unknown error" : error;
        job.markFailed(message);
        Job saved = jobRepository.save(job);
        JobMinutesMetering.record(billingMeter, saved);
        evaluateProductAlertsUseCase.execute(
                AlertEventType.JOB_FAILED,
                null,
                "Job " + saved.getType() + " failed: " + message,
                "job",
                saved.getId());
        return saved;
    }
}
