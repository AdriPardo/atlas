package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobStatus;
import com.atlas.domain.job.JobType;
import com.atlas.infrastructure.persistence.jpa.PageableFactory;
import com.atlas.infrastructure.persistence.jpa.entity.JobJpaEntity;
import com.atlas.infrastructure.persistence.jpa.mapper.JobJpaMapper;
import com.atlas.infrastructure.persistence.jpa.repository.JobJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
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
public class JobRepositoryAdapter implements JobRepositoryPort {

    private final JobJpaRepository repository;
    private final JobJpaMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Job save(Job job) {
        return mapper.toDomain(repository.save(mapper.toEntity(job)));
    }

    @Override
    public Optional<Job> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Job> search(JobStatus status, PageQuery pageQuery) {
        Specification<JobJpaEntity> specification = (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
        Page<JobJpaEntity> page = repository.findAll(specification, PageableFactory.from(pageQuery));
        List<Job> content = page.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(content, page.getNumber(), page.getSize(), page.getTotalElements(), pageQuery.sort());
    }

    @Override
    @Transactional
    public List<Job> claimPending(String workerId, int limit) {
        Query query = entityManager.createNativeQuery(
                """
                WITH claimed AS (
                    SELECT id
                    FROM jobs
                    WHERE status = 'PENDING'
                      AND available_at <= NOW()
                      AND attempts < max_attempts
                    ORDER BY available_at ASC, created_at ASC
                    FOR UPDATE SKIP LOCKED
                    LIMIT :limit
                )
                UPDATE jobs j
                SET status = 'RUNNING',
                    locked_at = NOW(),
                    locked_by = :workerId,
                    started_at = COALESCE(j.started_at, NOW()),
                    attempts = j.attempts + 1,
                    updated_at = NOW()
                FROM claimed
                WHERE j.id = claimed.id
                RETURNING j.id, j.type, j.payload, j.status, j.attempts, j.max_attempts,
                          j.available_at, j.locked_at, j.locked_by, j.started_at, j.finished_at,
                          j.last_error, j.created_at, j.updated_at
                """);
        query.setParameter("limit", limit);
        query.setParameter("workerId", workerId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<Job> claimed = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            claimed.add(mapRow(row));
        }
        entityManager.clear();
        return claimed;
    }

    @Override
    @Transactional
    public List<Job> findAndLockStaleRunning(Instant cutoff, int limit) {
        Query query = entityManager.createNativeQuery(
                """
                WITH stale AS (
                    SELECT id
                    FROM jobs
                    WHERE status = 'RUNNING'
                      AND locked_at IS NOT NULL
                      AND locked_at < :cutoff
                    ORDER BY locked_at ASC
                    FOR UPDATE SKIP LOCKED
                    LIMIT :limit
                )
                SELECT j.id, j.type, j.payload, j.status, j.attempts, j.max_attempts,
                       j.available_at, j.locked_at, j.locked_by, j.started_at, j.finished_at,
                       j.last_error, j.created_at, j.updated_at
                FROM jobs j
                INNER JOIN stale ON j.id = stale.id
                """);
        query.setParameter("cutoff", Timestamp.from(cutoff));
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<Job> stale = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            stale.add(mapRow(row));
        }
        return stale;
    }

    @Override
    @Transactional
    public boolean heartbeat(UUID jobId, String workerId) {
        Query query = entityManager.createNativeQuery(
                """
                UPDATE jobs
                SET locked_at = NOW(),
                    updated_at = NOW()
                WHERE id = :jobId
                  AND status = 'RUNNING'
                  AND locked_by = :workerId
                """);
        query.setParameter("jobId", jobId);
        query.setParameter("workerId", workerId);
        return query.executeUpdate() > 0;
    }

    @Override
    @Transactional
    public void updateLockedAt(UUID jobId, Instant lockedAt) {
        Query query = entityManager.createNativeQuery(
                """
                UPDATE jobs
                SET locked_at = :lockedAt,
                    updated_at = NOW()
                WHERE id = :jobId
                """);
        query.setParameter("jobId", jobId);
        query.setParameter("lockedAt", Timestamp.from(lockedAt));
        query.executeUpdate();
        entityManager.clear();
    }

    @Override
    @Transactional
    public int deleteTerminalOlderThan(Instant cutoff) {
        return repository.deleteByStatusInAndCreatedAtBefore(
                EnumSet.of(JobStatus.SUCCEEDED, JobStatus.FAILED, JobStatus.CANCELLED), cutoff);
    }

    private Job mapRow(Object[] row) {
        return Job.rehydrate(
                toUuid(row[0]),
                JobType.valueOf(String.valueOf(row[1])),
                String.valueOf(row[2]),
                JobStatus.valueOf(String.valueOf(row[3])),
                ((Number) row[4]).intValue(),
                ((Number) row[5]).intValue(),
                toInstant(row[6]),
                toInstant(row[7]),
                row[8] == null ? null : String.valueOf(row[8]),
                toInstant(row[9]),
                toInstant(row[10]),
                row[11] == null ? null : String.valueOf(row[11]),
                toInstant(row[12]),
                toInstant(row[13]));
    }

    private static UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(value));
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        return Instant.parse(String.valueOf(value));
    }
}
