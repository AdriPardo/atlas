package com.atlas.application.host;

import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncHostUseCase {

    private final HostRepositoryPort hostRepository;
    private final EnqueueJobUseCase enqueueJobUseCase;

    @Transactional
    public Job execute(UUID hostId) {
        if (hostRepository.findById(hostId).isEmpty()) {
            throw new NotFoundException("Host not found: " + hostId);
        }
        String payload = "{\"hostId\":\"" + hostId + "\"}";
        return enqueueJobUseCase.execute(new EnqueueJobUseCase.EnqueueJobCommand(JobType.SYNC_HOST, payload, 3));
    }
}
