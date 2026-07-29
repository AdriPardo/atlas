package com.atlas.application.port.out;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Records platform usage without blocking product flows. Soft limits may consult entitlements later;
 * v0.9 only appends meters.
 */
public interface BillingMeterPort {

    void record(String meter, BigDecimal quantity, Map<String, String> dimensions);
}
