package com.atlas.infrastructure.billing;

import com.atlas.application.billing.RecordUsageUseCase;
import com.atlas.application.port.out.BillingMeterPort;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-process meter: append-only usage rows. Never throws to callers (soft metering).
 */
@Component
@RequiredArgsConstructor
public class InProcessBillingMeterAdapter implements BillingMeterPort {

    private static final Logger log = LoggerFactory.getLogger(InProcessBillingMeterAdapter.class);

    private final RecordUsageUseCase recordUsageUseCase;

    @Override
    public void record(String meter, BigDecimal quantity, Map<String, String> dimensions) {
        try {
            recordUsageUseCase.execute(meter, quantity, dimensions);
        } catch (Exception ex) {
            log.warn("Billing meter failed for {}: {}", meter, ex.getMessage());
        }
    }
}
