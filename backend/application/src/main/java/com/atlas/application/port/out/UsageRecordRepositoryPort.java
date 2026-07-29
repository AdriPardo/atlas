package com.atlas.application.port.out;

import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.billing.UsageRecord;

public interface UsageRecordRepositoryPort {

    UsageRecord save(UsageRecord record);

    PageResult<UsageRecord> search(PageQuery pageQuery);
}
