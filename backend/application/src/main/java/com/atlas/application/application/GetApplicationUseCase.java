package com.atlas.application.application;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.domain.application.Application;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetApplicationUseCase {

    private final ApplicationRepositoryPort applicationRepository;

    @Transactional(readOnly = true)
    public Application execute(UUID id) {
        return applicationRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found: " + id));
    }
}
