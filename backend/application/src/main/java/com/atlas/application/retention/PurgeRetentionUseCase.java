package com.atlas.application.retention;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.application.port.out.PipelineRunRepositoryPort;
import com.atlas.application.port.out.RetentionPolicyPort;
import com.atlas.domain.shared.ForbiddenException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurgeRetentionUseCase {

    private final JobRepositoryPort jobRepository;
    private final PipelineRunRepositoryPort pipelineRunRepository;
    private final RetentionPolicyPort retentionPolicy;
    private final ProjectAuthorizationService authorizationService;
    private final RecordAuditUseCase recordAuditUseCase;

    /** Scheduled / internal path — uses configured retention policy. */
    @Transactional
    public PurgeResult execute() {
        if (!retentionPolicy.enabled()) {
            return new PurgeResult(0, 0, false);
        }
        return purge(retentionPolicy.jobsRetentionDays(), retentionPolicy.pipelineRunsRetentionDays(), "system");
    }

    /** ADMIN manual trigger. */
    @Transactional
    public PurgeResult executeAsAdmin() {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can purge retention data");
        }
        PurgeResult result =
                purge(retentionPolicy.jobsRetentionDays(), retentionPolicy.pipelineRunsRetentionDays(), actor.username());
        return result;
    }

    private PurgeResult purge(int jobsDays, int pipelineRunsDays, String actor) {
        int safeJobsDays = Math.max(1, jobsDays);
        int safeRunsDays = Math.max(1, pipelineRunsDays);
        Instant jobsCutoff = Instant.now().minus(safeJobsDays, ChronoUnit.DAYS);
        Instant runsCutoff = Instant.now().minus(safeRunsDays, ChronoUnit.DAYS);

        // Delete runs first so job_id FKs do not block job deletion.
        int deletedRuns = pipelineRunRepository.deleteTerminalOlderThan(runsCutoff);
        int deletedJobs = jobRepository.deleteTerminalOlderThan(jobsCutoff);

        recordAuditUseCase.execute(
                "RETENTION_PURGE",
                "system",
                null,
                "{\"deletedJobs\":"
                        + deletedJobs
                        + ",\"deletedPipelineRuns\":"
                        + deletedRuns
                        + ",\"jobsDays\":"
                        + safeJobsDays
                        + ",\"pipelineRunsDays\":"
                        + safeRunsDays
                        + ",\"actor\":\""
                        + actor
                        + "\"}");

        return new PurgeResult(deletedJobs, deletedRuns, true);
    }

    public record PurgeResult(int deletedJobs, int deletedPipelineRuns, boolean ran) {}
}
