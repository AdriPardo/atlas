package com.atlas.application.job;

import com.atlas.application.port.out.BillingMeterPort;
import com.atlas.domain.billing.UsageMeters;
import com.atlas.domain.job.Job;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Soft-meters wall-clock job runtime as {@link UsageMeters#JOB_MINUTES}. Never throws. */
final class JobMinutesMetering {

    private static final BigDecimal MILLIS_PER_MINUTE = BigDecimal.valueOf(60_000L);

    private JobMinutesMetering() {}

    static void record(BillingMeterPort billingMeter, Job job) {
        if (billingMeter == null || job == null) {
            return;
        }
        if (job.getStartedAt() == null || job.getFinishedAt() == null) {
            return;
        }
        long millis = Duration.between(job.getStartedAt(), job.getFinishedAt()).toMillis();
        if (millis < 0) {
            return;
        }
        BigDecimal minutes =
                BigDecimal.valueOf(millis).divide(MILLIS_PER_MINUTE, 4, RoundingMode.HALF_UP);
        Map<String, String> dimensions = new LinkedHashMap<>();
        dimensions.put("jobId", job.getId().toString());
        dimensions.put("jobType", job.getType().name());
        dimensions.put("status", job.getStatus().name());
        billingMeter.record(UsageMeters.JOB_MINUTES, minutes, dimensions);
    }
}
