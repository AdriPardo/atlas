package com.atlas.application.port.out;

import com.atlas.domain.application.Application;
import com.atlas.domain.application.ApplicationStatus;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepositoryPort {

    Application save(Application application);

    Optional<Application> findById(UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    PageResult<Application> search(String name, ApplicationStatus status, PageQuery pageQuery);

    void deleteById(UUID id);

    long count();
}
