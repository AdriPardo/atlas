package com.atlas.application.service;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListServicesUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public PageResult<ServiceUnit> execute(
            UUID projectId, String name, ServiceStatus status, PageQuery pageQuery) {
        if (projectId != null) {
            authorizationService.require(projectId, ProjectPermission.READ);
        } else {
            authorizationService.requireActor();
        }
        return serviceRepository.search(projectId, name, status, pageQuery);
    }
}
