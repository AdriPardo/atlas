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
public class GetJobUseCase {

    private final JobRepositoryPort jobRepository;

    @Transactional(readOnly = true)
    public Job execute(UUID id) {
        return jobRepository.findById(id).orElseThrow(() -> new NotFoundException("Job not found: " + id));
    }
}
