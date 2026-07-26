package com.atlas.application.job;

import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.domain.job.Job;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FailJobUseCase {

    private final JobRepositoryPort jobRepository;

    @Transactional
    public Job execute(UUID jobId, String error) {
        Job job = jobRepository
                .findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found: " + jobId));
        job.markFailed(error == null ? "unknown error" : error);
        return jobRepository.save(job);
    }
}
