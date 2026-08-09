package com.atlas.api.web;

import com.atlas.application.database.RedeemPgwebConsoleTicketUseCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * pgweb Connect Backend callback (server-to-server). Auth = shared {@code token} in body, not JWT.
 */
@RestController
@RequestMapping("/api/v1/internal/pgweb")
@RequiredArgsConstructor
@Hidden
public class PgwebConnectBackendController {

    private final RedeemPgwebConsoleTicketUseCase redeemPgwebConsoleTicketUseCase;

    public record ConnectRequest(String resource, String token) {}

    public record ConnectResponse(@JsonProperty("database_url") String databaseUrl) {}

    @PostMapping("/connect")
    public ResponseEntity<ConnectResponse> connect(@RequestBody ConnectRequest body) {
        String resource = body == null ? null : body.resource();
        String token = body == null ? null : body.token();
        String databaseUrl = redeemPgwebConsoleTicketUseCase.redeem(resource, token);
        return ResponseEntity.ok(new ConnectResponse(databaseUrl));
    }
}
