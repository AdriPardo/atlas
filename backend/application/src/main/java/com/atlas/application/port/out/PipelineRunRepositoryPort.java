package com.atlas.application.port.out;

import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.pipeline.PipelineRun;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PipelineRunRepositoryPort {

    PipelineRun save(PipelineRun run);

    Optional<PipelineRun> findById(UUID id);

    PageResult<PipelineRun> searchByPipelineId(UUID pipelineId, PageQuery pageQuery);

    /** Deletes terminal pipeline runs created before cutoff. */
    int deleteTerminalOlderThan(Instant cutoff);
}
