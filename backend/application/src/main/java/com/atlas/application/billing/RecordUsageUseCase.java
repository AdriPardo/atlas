package com.atlas.application.billing;

import com.atlas.application.port.out.UsageRecordRepositoryPort;
import com.atlas.domain.billing.UsageRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordUsageUseCase {

    private final UsageRecordRepositoryPort usageRecordRepository;

    @Transactional
    public UsageRecord execute(String meter, BigDecimal quantity, Map<String, String> dimensions) {
        Instant periodStart = monthStartUtc();
        Instant periodEnd = monthEndUtc();
        String dimsJson = toJson(dimensions == null ? Map.of() : dimensions);
        return usageRecordRepository.save(
                UsageRecord.record(meter, quantity, periodStart, periodEnd, dimsJson));
    }

    static Instant monthStartUtc() {
        LocalDate first = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.firstDayOfMonth());
        return first.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    static Instant monthEndUtc() {
        LocalDate last = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.lastDayOfMonth());
        return last.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1);
    }

    private static String toJson(Map<String, String> dimensions) {
        if (dimensions.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : dimensions.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"')
                    .append(escape(entry.getKey()))
                    .append("\":\"")
                    .append(escape(entry.getValue() == null ? "" : entry.getValue()))
                    .append('"');
        }
        return sb.append('}').toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
