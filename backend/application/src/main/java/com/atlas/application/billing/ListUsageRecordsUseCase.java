package com.atlas.application.billing;

import com.atlas.application.access.FeatureGateService;
import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.UsageRecordRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.billing.FeatureFlags;
import com.atlas.domain.billing.UsageRecord;
import com.atlas.domain.shared.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListUsageRecordsUseCase {

    private final UsageRecordRepositoryPort usageRecordRepository;
    private final ProjectAuthorizationService authorizationService;
    private final FeatureGateService featureGate;

    @Transactional(readOnly = true)
    public PageResult<UsageRecord> execute(PageQuery pageQuery) {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can read billing usage");
        }
        featureGate.require(FeatureFlags.BILLING);
        return usageRecordRepository.search(pageQuery);
    }
}
