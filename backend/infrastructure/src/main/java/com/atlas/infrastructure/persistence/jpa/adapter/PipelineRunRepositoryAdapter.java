package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.PipelineRunRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.pipeline.PipelineRun;
import com.atlas.infrastructure.persistence.jpa.PageableFactory;
import com.atlas.infrastructure.persistence.jpa.entity.PipelineRunJpaEntity;
import com.atlas.infrastructure.persistence.jpa.mapper.PipelineRunJpaMapper;
import com.atlas.infrastructure.persistence.jpa.repository.PipelineRunJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PipelineRunRepositoryAdapter implements PipelineRunRepositoryPort {

    private final PipelineRunJpaRepository repository;
    private final PipelineRunJpaMapper mapper;

    @Override
    public PipelineRun save(PipelineRun run) {
        return mapper.toDomain(repository.save(mapper.toEntity(run)));
    }

    @Override
    public Optional<PipelineRun> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<PipelineRun> searchByPipelineId(UUID pipelineId, PageQuery pageQuery) {
        Specification<PipelineRunJpaEntity> specification =
                (root, query, cb) -> cb.equal(root.get("pipelineId"), pipelineId);
        Page<PipelineRunJpaEntity> page = repository.findAll(specification, PageableFactory.from(pageQuery));
        List<PipelineRun> content = page.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(content, page.getNumber(), page.getSize(), page.getTotalElements(), pageQuery.sort());
    }

    @Override
    @Transactional
    public int deleteTerminalOlderThan(Instant cutoff) {
        return repository.deleteByStatusInAndCreatedAtBefore(
                List.of("SUCCEEDED", "FAILED", "CANCELLED"), cutoff);
    }
}
