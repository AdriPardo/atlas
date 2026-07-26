package com.atlas.platform.application.usecase.application;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.model.Application;
import com.atlas.platform.domain.model.ApplicationStatus;
import com.atlas.platform.domain.model.PageResult;
import com.atlas.platform.domain.port.out.ApplicationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListApplicationsUseCase {

    private final ApplicationRepositoryPort applicationRepository;

    public ListApplicationsUseCase(ApplicationRepositoryPort applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<Application> execute(
            String name,
            ApplicationStatus status,
            int page,
            int size,
            String sortBy,
            boolean ascending) {
        return applicationRepository.search(
                InstallationContext.currentInstallationId(),
                name,
                status,
                page,
                size,
                sortBy,
                ascending);
    }
}
