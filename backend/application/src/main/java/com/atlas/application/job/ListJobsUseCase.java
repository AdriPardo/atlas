package com.atlas.application.job;

import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListJobsUseCase {

    private final JobRepositoryPort jobRepository;

    @Transactional(readOnly = true)
    public PageResult<Job> execute(JobStatus status, PageQuery pageQuery) {
        return jobRepository.search(status, pageQuery);
    }
}
