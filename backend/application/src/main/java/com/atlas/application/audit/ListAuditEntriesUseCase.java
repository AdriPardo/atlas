package com.atlas.application.audit;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.AuditRepositoryPort;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.audit.AuditEntry;
import com.atlas.domain.shared.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAuditEntriesUseCase {

    private final AuditRepositoryPort auditRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public PageResult<AuditEntry> execute(PageQuery pageQuery) {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can read audit log");
        }
        return auditRepository.search(pageQuery);
    }
}
