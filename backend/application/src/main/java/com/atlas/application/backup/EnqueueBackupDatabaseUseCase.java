package com.atlas.application.backup;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.application.port.out.BackupPolicyPort;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnqueueBackupDatabaseUseCase {

    private final EnqueueJobUseCase enqueueJobUseCase;
    private final BackupPolicyPort backupPolicy;
    private final ProjectAuthorizationService authorizationService;
    private final RecordAuditUseCase recordAuditUseCase;

    /** Scheduled / internal path. */
    @Transactional
    public Job execute() {
        if (!backupPolicy.enabled()) {
            throw new DomainException("Database backups are disabled (atlas.backup.enabled=false)");
        }
        return enqueue("system");
    }

    /** ADMIN manual trigger. */
    @Transactional
    public Job executeAsAdmin() {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can trigger database backups");
        }
        if (!backupPolicy.enabled()) {
            throw new DomainException("Database backups are disabled (atlas.backup.enabled=false)");
        }
        return enqueue(actor.username());
    }

    private Job enqueue(String actor) {
        String payload = "{\"directory\":\"" + escape(backupPolicy.directory()) + "\",\"keepCount\":"
                + backupPolicy.keepCount() + "}";
        Job job = enqueueJobUseCase.execute(
                new EnqueueJobUseCase.EnqueueJobCommand(JobType.BACKUP_DATABASE, payload, 2));
        recordAuditUseCase.execute(
                "BACKUP_DATABASE",
                "job",
                job.getId(),
                "{\"actor\":\"" + escape(actor) + "\",\"directory\":\"" + escape(backupPolicy.directory())
                        + "\"}");
        return job;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
