package com.atlas.application.pipeline;

import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.pipeline.Pipeline;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListPipelinesUseCase {

    private final PipelineRepositoryPort pipelineRepository;

    @Transactional(readOnly = true)
    public PageResult<Pipeline> execute(UUID projectId, String name, PageQuery pageQuery) {
        return pipelineRepository.search(projectId, name, pageQuery);
    }
}
