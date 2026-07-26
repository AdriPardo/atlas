package com.atlas.application.job;

import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.domain.job.Job;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClaimJobsUseCase {

    private final JobRepositoryPort jobRepository;

    @Transactional
    public List<Job> execute(String workerId, int limit) {
        int batch = limit < 1 ? 1 : Math.min(limit, 50);
        return jobRepository.claimPending(workerId, batch);
    }
}
