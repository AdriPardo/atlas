package com.atlas.infrastructure.adapter.database;

import com.atlas.application.port.out.PgwebConsoleTicketPort;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Process-local one-shot tickets for pgweb Connect Backend. Fine for single-replica Atlas backend.
 */
@Component
public class InMemoryPgwebConsoleTicketAdapter implements PgwebConsoleTicketPort {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ENTRIES = 10_000;

    private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryPgwebConsoleTicketAdapter() {
        this(Clock.systemUTC());
    }

    InMemoryPgwebConsoleTicketAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String issue(String connectionUrl, Instant redeemBy) {
        if (connectionUrl == null || connectionUrl.isBlank()) {
            throw new IllegalArgumentException("connectionUrl is required");
        }
        if (redeemBy == null || !redeemBy.isAfter(clock.instant())) {
            throw new IllegalArgumentException("redeemBy must be in the future");
        }
        purgeExpired();
        if (tickets.size() >= MAX_ENTRIES) {
            throw new IllegalStateException("pgweb console ticket store full");
        }
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String id = HexFormat.of().formatHex(bytes);
        tickets.put(id, new Ticket(connectionUrl, redeemBy));
        return id;
    }

    @Override
    public Optional<String> redeem(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return Optional.empty();
        }
        Ticket ticket = tickets.remove(resourceId.trim());
        if (ticket == null) {
            return Optional.empty();
        }
        if (!ticket.redeemBy().isAfter(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(ticket.connectionUrl());
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        Iterator<Map.Entry<String, Ticket>> it = tickets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Ticket> entry = it.next();
            if (!entry.getValue().redeemBy().isAfter(now)) {
                it.remove();
            }
        }
    }

    private record Ticket(String connectionUrl, Instant redeemBy) {}
}
