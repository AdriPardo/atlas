package com.atlas.application.port.out;

import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.audit.AuditEntry;

public interface AuditRepositoryPort {

    AuditEntry save(AuditEntry entry);

    PageResult<AuditEntry> search(PageQuery pageQuery);
}
