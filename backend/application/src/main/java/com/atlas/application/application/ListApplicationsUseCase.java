package com.atlas.application.application;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.application.Application;
import com.atlas.domain.application.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListApplicationsUseCase {

    private final ApplicationRepositoryPort applicationRepository;

    @Transactional(readOnly = true)
    public PageResult<Application> execute(String name, ApplicationStatus status, PageQuery pageQuery) {
        return applicationRepository.search(name, status, pageQuery);
    }
}
