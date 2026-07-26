package com.atlas.application.port.out;

import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepositoryPort {

    ServiceUnit save(ServiceUnit service);

    Optional<ServiceUnit> findById(UUID id);

    Optional<ServiceUnit> findDefaultByProjectId(UUID projectId);

    List<ServiceUnit> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndName(UUID projectId, String name);

    boolean existsByProjectIdAndNameAndIdNot(UUID projectId, String name, UUID id);

    PageResult<ServiceUnit> search(UUID projectId, String name, ServiceStatus status, PageQuery pageQuery);

    void deleteById(UUID id);

    boolean existsByProjectId(UUID projectId);

    long count();
}
