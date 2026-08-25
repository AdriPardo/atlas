package com.atlas.infrastructure.mail;

import com.atlas.application.port.out.ProjectMailRateLimitPort;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryProjectMailRateLimiter implements ProjectMailRateLimitPort {

    private final Map<BucketKey, Integer> counts = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(UUID projectId, int dailyLimit) {
        BucketKey key = new BucketKey(projectId, LocalDate.now(ZoneOffset.UTC));
        int next = counts.merge(key, 1, Integer::sum);
        if (next > dailyLimit) {
            counts.merge(key, -1, Integer::sum);
            return false;
        }
        return true;
    }

    @Override
    public int remainingToday(UUID projectId, int dailyLimit) {
        BucketKey key = new BucketKey(projectId, LocalDate.now(ZoneOffset.UTC));
        int used = counts.getOrDefault(key, 0);
        return Math.max(0, dailyLimit - used);
    }

    private record BucketKey(UUID projectId, LocalDate day) {}
}
