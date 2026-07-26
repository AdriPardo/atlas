package com.atlas.application.job;

import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnqueueJobUseCase {

    private final JobRepositoryPort jobRepository;

    @Transactional
    public Job execute(EnqueueJobCommand command) {
        int maxAttempts = command.maxAttempts() == null || command.maxAttempts() < 1 ? 3 : command.maxAttempts();
        Job job = Job.enqueue(command.type(), command.payload(), maxAttempts);
        return jobRepository.save(job);
    }

    public record EnqueueJobCommand(JobType type, String payload, Integer maxAttempts) {}
}
