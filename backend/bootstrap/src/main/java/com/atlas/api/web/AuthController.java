package com.atlas.api.web;

import com.atlas.api.dto.request.LoginRequest;
import com.atlas.api.dto.response.LoginResponse;
import com.atlas.application.auth.AuthenticateFromAuthentikUseCase;
import com.atlas.application.auth.AuthenticateFromAuthentikUseCase.AuthentikIdentity;
import com.atlas.application.auth.AuthenticateUserUseCase;
import com.atlas.domain.shared.UnauthorizedException;
import com.atlas.infrastructure.security.AtlasAuthCookieNames;
import com.atlas.infrastructure.security.AuthentikHeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String OUTPOST_START_PATH = "/outpost.goauthentik.io/start";
    private static final String SSO_PATH = "/api/v1/auth/sso";

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final AuthenticateFromAuthentikUseCase authenticateFromAuthentikUseCase;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authenticateUserUseCase.execute(
                new AuthenticateUserUseCase.AuthenticateCommand(request.username(), request.password()));
        return ResponseEntity.ok(new LoginResponse(result.accessToken(), result.tokenType(), result.expiresIn()));
    }

    /**
     * Prod SSO mint — browser GET only (Traefik ForwardAuth injects X-authentik-*).
     * Sets session cookie and redirects to SPA. Never use fetch/XHR for this endpoint.
     */
    @GetMapping("/sso")
    public void ssoGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = header(request, AuthentikHeaderNames.USERNAME);
        if (username == null || username.isBlank()) {
            String returnTo = buildAbsoluteUrl(request, SSO_PATH);
            response.sendRedirect(
                    OUTPOST_START_PATH + "?rd=" + URLEncoder.encode(returnTo, StandardCharsets.UTF_8));
            return;
        }

        try {
            var result = authenticateFromAuthentikUseCase.execute(new AuthentikIdentity(
                    username,
                    header(request, AuthentikHeaderNames.GROUPS),
                    header(request, AuthentikHeaderNames.EMAIL),
                    header(request, AuthentikHeaderNames.NAME),
                    header(request, AuthentikHeaderNames.UID)));
            ResponseCookie cookie = ResponseCookie.from(AtlasAuthCookieNames.TOKEN, result.accessToken())
                    .path("/")
                    .maxAge(Duration.ofSeconds(result.expiresIn()))
                    .secure(true)
                    .sameSite("Lax")
                    .httpOnly(false)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            response.sendRedirect("/");
        } catch (UnauthorizedException ex) {
            String returnTo = buildAbsoluteUrl(request, SSO_PATH);
            response.sendRedirect(
                    OUTPOST_START_PATH + "?rd=" + URLEncoder.encode(returnTo, StandardCharsets.UTF_8));
        }
    }

    @PostMapping("/sso")
    public ResponseEntity<LoginResponse> ssoPost(HttpServletRequest request) {
        var result = authenticateFromAuthentikUseCase.execute(new AuthentikIdentity(
                header(request, AuthentikHeaderNames.USERNAME),
                header(request, AuthentikHeaderNames.GROUPS),
                header(request, AuthentikHeaderNames.EMAIL),
                header(request, AuthentikHeaderNames.NAME),
                header(request, AuthentikHeaderNames.UID)));
        return ResponseEntity.ok(new LoginResponse(result.accessToken(), result.tokenType(), result.expiresIn()));
    }

    static String buildAbsoluteUrl(HttpServletRequest request, String path) {
        String scheme = forwarded(request, "X-Forwarded-Proto", request.getScheme());
        String host = forwarded(request, "X-Forwarded-Host", request.getHeader("Host"));
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        return scheme + "://" + host + path;
    }

    private static String forwarded(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? fallback : value.split(",")[0].trim();
    }

    private static String header(HttpServletRequest request, String name) {
        return request.getHeader(name);
    }
}
