package com.atlas.application.audit;

import com.atlas.application.port.out.AuditRepositoryPort;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.domain.audit.AuditEntry;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordAuditUseCase {

    private final AuditRepositoryPort auditRepository;
    private final CurrentUserPort currentUserPort;

    @Transactional
    public AuditEntry execute(String action, String resourceType, UUID resourceId, String metadata) {
        CurrentUserPort.Actor actor = currentUserPort
                .current()
                .orElse(new CurrentUserPort.Actor(null, "system", com.atlas.domain.user.Role.OPERATOR));
        return auditRepository.save(AuditEntry.record(
                actor.id(), actor.username(), action, resourceType, resourceId, metadata));
    }
}
