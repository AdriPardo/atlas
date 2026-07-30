package com.atlas.application.audit;

import com.atlas.application.access.FeatureGateService;
import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.AuditRepositoryPort;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.audit.AuditEntry;
import com.atlas.domain.billing.FeatureFlags;
import com.atlas.domain.shared.ForbiddenException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enterprise audit JSON export — capped to keep payloads bounded. */
@Service
@RequiredArgsConstructor
public class ExportAuditEntriesUseCase {

    static final int MAX_PAGES = 10;
    static final int PAGE_SIZE = 100;

    private final AuditRepositoryPort auditRepository;
    private final ProjectAuthorizationService authorizationService;
    private final FeatureGateService featureGate;

    @Transactional(readOnly = true)
    public List<AuditEntry> execute() {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can export audit log");
        }
        featureGate.require(FeatureFlags.AUDIT_EXPORT);

        List<AuditEntry> entries = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            PageResult<AuditEntry> result =
                    auditRepository.search(new PageQuery(page, PAGE_SIZE, "createdAt,desc"));
            entries.addAll(result.content());
            if (result.content().size() < PAGE_SIZE || entries.size() >= result.totalElements()) {
                break;
            }
        }
        return entries;
    }
}
