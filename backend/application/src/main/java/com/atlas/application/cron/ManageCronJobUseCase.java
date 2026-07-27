package com.atlas.application.cron;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.CronJobRepositoryPort;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.cron.CronJob;
import com.atlas.domain.cron.CronTargetType;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.user.Role;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageCronJobUseCase {

    private final CronJobRepositoryPort cronJobRepository;
    private final HostRepositoryPort hostRepository;
    private final ProjectAuthorizationService authorizationService;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional(readOnly = true)
    public List<CronJob> list() {
        requireOperatorOrAdmin();
        return cronJobRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CronJob get(UUID cronJobId) {
        requireOperatorOrAdmin();
        return requireCron(cronJobId);
    }

    @Transactional
    public CronJob create(String name, String cronExpression, CronTargetType targetType, UUID targetId) {
        requireOperatorOrAdmin();
        validateCron(cronExpression);
        validateTarget(targetType, targetId);
        if (cronJobRepository.existsByNameIgnoreCase(name.trim())) {
            throw new ConflictException("Cron job name already exists");
        }
        CronJob saved = cronJobRepository.save(CronJob.create(name, cronExpression, targetType, targetId));
        recordAuditUseCase.execute(
                "CRON_JOB_CREATE",
                "cron_job",
                saved.getId(),
                "{\"name\":\"" + saved.getName() + "\",\"targetType\":\"" + saved.getTargetType() + "\"}");
        return saved;
    }

    @Transactional
    public CronJob update(
            UUID cronJobId,
            String name,
            String cronExpression,
            CronTargetType targetType,
            UUID targetId,
            Boolean enabled) {
        requireOperatorOrAdmin();
        validateCron(cronExpression);
        validateTarget(targetType, targetId);
        CronJob cronJob = requireCron(cronJobId);
        if (cronJobRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), cronJobId)) {
            throw new ConflictException("Cron job name already exists");
        }
        cronJob.update(name, cronExpression, targetType, targetId, enabled);
        CronJob saved = cronJobRepository.save(cronJob);
        recordAuditUseCase.execute(
                "CRON_JOB_UPDATE",
                "cron_job",
                saved.getId(),
                "{\"name\":\"" + saved.getName() + "\",\"enabled\":" + saved.isEnabled() + "}");
        return saved;
    }

    @Transactional
    public void delete(UUID cronJobId) {
        requireOperatorOrAdmin();
        CronJob cronJob = requireCron(cronJobId);
        cronJobRepository.deleteById(cronJobId);
        recordAuditUseCase.execute(
                "CRON_JOB_DELETE", "cron_job", cronJobId, "{\"name\":\"" + cronJob.getName() + "\"}");
    }

    static void validateCron(String expression) {
        try {
            CronExpression.parse(expression.trim());
        } catch (Exception ex) {
            throw new DomainException("Invalid Spring cron expression (6 fields): " + ex.getMessage());
        }
    }

    private void validateTarget(CronTargetType targetType, UUID targetId) {
        if (targetType == CronTargetType.SYNC_HOST) {
            if (targetId == null || hostRepository.findById(targetId).isEmpty()) {
                throw new NotFoundException("Host not found: " + targetId);
            }
        }
    }

    private void requireOperatorOrAdmin() {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin() && actor.role() != Role.OPERATOR) {
            throw new ForbiddenException("ADMIN or OPERATOR required");
        }
    }

    private CronJob requireCron(UUID cronJobId) {
        return cronJobRepository
                .findById(cronJobId)
                .orElseThrow(() -> new NotFoundException("Cron job not found: " + cronJobId));
    }
}
