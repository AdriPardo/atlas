package com.atlas.infrastructure.webhook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryWebhookRateLimiterTest {

    @Test
    void allowsUntilMaxThenBlocks() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        InMemoryWebhookRateLimiter limiter = new InMemoryWebhookRateLimiter(3, 60, clock);

        assertTrue(limiter.tryAcquire("t1"));
        assertTrue(limiter.tryAcquire("t1"));
        assertTrue(limiter.tryAcquire("t1"));
        assertFalse(limiter.tryAcquire("t1"));

        clock.advanceSeconds(61);
        assertTrue(limiter.tryAcquire("t1"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
