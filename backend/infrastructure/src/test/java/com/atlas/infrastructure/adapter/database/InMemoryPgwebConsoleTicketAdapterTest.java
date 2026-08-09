package com.atlas.infrastructure.adapter.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class InMemoryPgwebConsoleTicketAdapterTest {

    @Test
    void issueAndRedeemOnce() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC);
        InMemoryPgwebConsoleTicketAdapter store = new InMemoryPgwebConsoleTicketAdapter(clock);
        String id = store.issue(
                "postgresql://u:p@postgres:5432/apps?sslmode=disable",
                Instant.parse("2026-08-09T12:02:00Z"));
        assertEquals(
                "postgresql://u:p@postgres:5432/apps?sslmode=disable",
                store.redeem(id).orElseThrow());
        assertTrue(store.redeem(id).isEmpty());
    }

    @Test
    void expiredTicketNotRedeemable() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-09T12:00:00Z"));
        InMemoryPgwebConsoleTicketAdapter store = new InMemoryPgwebConsoleTicketAdapter(clock);
        String id = store.issue("postgresql://u:p@postgres:5432/apps", clock.instant().plus(2, ChronoUnit.MINUTES));
        clock.set(Instant.parse("2026-08-09T12:03:00Z"));
        assertTrue(store.redeem(id).isEmpty());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
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
