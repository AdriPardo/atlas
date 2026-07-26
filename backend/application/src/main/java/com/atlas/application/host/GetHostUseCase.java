package com.atlas.application.host;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetHostUseCase {

    private final HostRepositoryPort hostRepository;

    @Transactional(readOnly = true)
    public Host execute(UUID id) {
        return hostRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Host not found: " + id));
    }
}
