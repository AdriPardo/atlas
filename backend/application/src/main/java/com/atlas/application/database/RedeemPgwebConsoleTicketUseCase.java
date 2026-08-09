package com.atlas.application.database;

import com.atlas.application.port.out.DbConsoleConfigPort;
import com.atlas.application.port.out.PgwebConsoleTicketPort;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * pgweb Connect Backend callback: validate shared token + redeem one-time resource → database_url.
 */
@Service
@RequiredArgsConstructor
public class RedeemPgwebConsoleTicketUseCase {

    private final PgwebConsoleTicketPort tickets;
    private final DbConsoleConfigPort dbConsoleConfig;

    public String redeem(String resourceId, String presentedToken) {
        String expected = dbConsoleConfig
                .connectToken()
                .orElseThrow(() -> new DomainException(
                        "DB console connect token not configured — set ATLAS_DB_CONSOLE_CONNECT_TOKEN"));
        if (presentedToken == null || presentedToken.isBlank() || !constantTimeEquals(expected, presentedToken)) {
            throw new ForbiddenException("Invalid pgweb connect token");
        }
        return tickets
                .redeem(resourceId)
                .orElseThrow(() -> new NotFoundException("Console ticket unknown, expired, or already used"));
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
