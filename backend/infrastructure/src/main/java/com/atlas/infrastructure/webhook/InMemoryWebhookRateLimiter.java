package com.atlas.infrastructure.webhook;

import com.atlas.application.port.out.WebhookRateLimiterPort;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Lightweight sliding-window limiter for public git webhooks (per token).
 * Not distributed — sufficient for single-node / VM deploy; document Redis for HA.
 */
@Component
public class InMemoryWebhookRateLimiter implements WebhookRateLimiterPort {

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;

    @Autowired
    public InMemoryWebhookRateLimiter(
            @Value("${atlas.webhooks.rate-limit.max-requests:30}") int maxRequests,
            @Value("${atlas.webhooks.rate-limit.window-seconds:60}") int windowSeconds) {
        this(maxRequests, windowSeconds, Clock.systemUTC());
    }

    InMemoryWebhookRateLimiter(int maxRequests, int windowSeconds, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire(String key) {
        long now = clock.millis();
        Deque<Long> timestamps = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMillis) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
