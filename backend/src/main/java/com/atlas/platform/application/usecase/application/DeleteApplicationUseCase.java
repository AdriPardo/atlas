package com.atlas.platform.application.usecase.application;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.exception.NotFoundException;
import com.atlas.platform.domain.model.Application;
import com.atlas.platform.domain.port.out.ApplicationRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteApplicationUseCase {

    private final ApplicationRepositoryPort applicationRepository;

    public DeleteApplicationUseCase(ApplicationRepositoryPort applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public void execute(UUID id) {
        Application application = applicationRepository
                .findById(InstallationContext.currentInstallationId(), id)
                .orElseThrow(() -> new NotFoundException("Application not found: " + id));
        applicationRepository.delete(application);
    }
}
