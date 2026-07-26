package com.atlas.application.port.out;

import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.pipeline.Pipeline;
import java.util.Optional;
import java.util.UUID;

public interface PipelineRepositoryPort {

    Pipeline save(Pipeline pipeline);

    Optional<Pipeline> findById(UUID id);

    boolean existsByProjectIdAndName(UUID projectId, String name);

    boolean existsByProjectIdAndNameAndIdNot(UUID projectId, String name, UUID id);

    PageResult<Pipeline> search(UUID projectId, String name, PageQuery pageQuery);

    void deleteById(UUID id);
}
