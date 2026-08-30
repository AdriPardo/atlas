package com.atlas.api.web;

import com.atlas.application.auth.AuthenticateFromAuthentikUseCase;
import com.atlas.application.auth.AuthenticateFromAuthentikUseCase.AuthentikIdentity;
import com.atlas.domain.shared.UnauthorizedException;
import com.atlas.infrastructure.security.AtlasAuthCookieNames;
import com.atlas.infrastructure.security.AuthentikHeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Browser-navigation SSO bootstrap. Traefik ForwardAuth injects {@code X-authentik-*} only on
 * document requests — not on SPA XHR — so prod mints JWT via full-page GET here, sets a session
 * cookie, then redirects back to the SPA (no inline JS — survives CSP on securityHeaders).
 */
@Controller
@RequiredArgsConstructor
public class SsoBootstrapController {

    private static final Logger log = LoggerFactory.getLogger(SsoBootstrapController.class);

    /** Public browser path — Traefik atlas router (not /api/) so ForwardAuth returns 302, not 403. */
    public static final String PUBLIC_BOOTSTRAP_PATH = "/auth/sso/bootstrap";

    private static final String OUTPOST_START_PATH = "/outpost.goauthentik.io/start";

    /** Must match {@code frontend/src/shared/api/tokenStorage.ts}. */
    static final String TOKEN_STORAGE_KEY = AtlasAuthCookieNames.TOKEN;

    private static final String SSO_ERROR_STORAGE_KEY = "atlas.sso.error";
    static final String SSO_REDIRECT_STORAGE_KEY = "atlas.sso.redirect";
    static final String TOKEN_HASH_PREFIX = "#atlas.token=";

    private final AuthenticateFromAuthentikUseCase authenticateFromAuthentikUseCase;

    @GetMapping({PUBLIC_BOOTSTRAP_PATH, "/api/v1/auth/sso/bootstrap"})
    public void bootstrap(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(name = "returnTo", defaultValue = "/") String returnTo)
            throws IOException {
        String safeReturnTo = sanitizeReturnTo(returnTo);
        String username = header(request, AuthentikHeaderNames.USERNAME);
        String email = header(request, AuthentikHeaderNames.EMAIL);
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasGroups = header(request, AuthentikHeaderNames.GROUPS) != null;

        log.info(
                "SSO bootstrap: usernamePresent={} groupsPresent={} emailDomain={} returnTo={}",
                hasUsername,
                hasGroups,
                emailDomain(email),
                safeReturnTo);

        if (!hasUsername) {
            String bootstrapUrl = buildPublicBootstrapUrl(request, safeReturnTo);
            String location = OUTPOST_START_PATH + "?rd=" + URLEncoder.encode(bootstrapUrl, StandardCharsets.UTF_8);
            log.info("SSO bootstrap: no ForwardAuth headers — redirecting to Authentik outpost");
            response.sendRedirect(location);
            return;
        }

        try {
            var result = authenticateFromAuthentikUseCase.execute(new AuthentikIdentity(
                    username,
                    header(request, AuthentikHeaderNames.GROUPS),
                    email,
                    header(request, AuthentikHeaderNames.NAME),
                    header(request, AuthentikHeaderNames.UID)));
            log.info("SSO bootstrap: JWT minted for username={}", username.trim());
            completeBootstrapSession(response, result.accessToken(), result.expiresIn(), safeReturnTo);
        } catch (UnauthorizedException ex) {
            String code = mapFailureCode(ex);
            log.warn("SSO bootstrap failed for username={}: {} ({})", username.trim(), code, ex.getMessage());
            writeBootstrapErrorHtml(response, code, ex.getMessage(), safeReturnTo);
        }
    }

    static String mapFailureCode(UnauthorizedException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("not enabled")) {
            return "sso_disabled";
        }
        if (message.contains("missing")) {
            return "identity_missing";
        }
        return "mint_failed";
    }

    static String sanitizeReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return "/";
        }
        String trimmed = returnTo.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return "/";
        }
        return trimmed;
    }

    static String toJsString(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '<' -> sb.append("\\u003c");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String emailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "n/a";
        }
        return email.substring(email.indexOf('@') + 1);
    }

    static String buildPublicBootstrapUrl(HttpServletRequest request, String returnTo) {
        String scheme = forwarded(request, "X-Forwarded-Proto", request.getScheme());
        String host = forwarded(request, "X-Forwarded-Host", request.getHeader("Host"));
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        String encodedReturnTo = URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
        return scheme + "://" + host + PUBLIC_BOOTSTRAP_PATH + "?returnTo=" + encodedReturnTo;
    }

    private static String forwarded(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? fallback : value.split(",")[0].trim();
    }

    private void completeBootstrapSession(
            HttpServletResponse response, String accessToken, long maxAgeSeconds, String returnTo)
            throws IOException {
        ResponseCookie cookie = ResponseCookie.from(TOKEN_STORAGE_KEY, accessToken)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .secure(true)
                .sameSite("Lax")
                .httpOnly(false)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        String location = returnTo + TOKEN_HASH_PREFIX + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        response.sendRedirect(location);
    }

    private static void writeBootstrapErrorHtml(
            HttpServletResponse response, String errorCode, String detail, String returnTo)
            throws IOException {
        String html =
                """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Error de inicio de sesión</title>
                </head>
                <body>
                  <h1>No se pudo completar el inicio de sesión</h1>
                  <p>%s</p>
                  <p><a href="%s">Volver a Atlas</a></p>
                  <script>
                    sessionStorage.removeItem(%s);
                    sessionStorage.setItem(%s, %s);
                  </script>
                </body>
                </html>
                """
                        .formatted(
                                escapeHtml(detail),
                                escapeHtml(returnTo),
                                toJsString(SSO_REDIRECT_STORAGE_KEY),
                                toJsString(SSO_ERROR_STORAGE_KEY),
                                toJsString(errorCode));
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(html);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String header(HttpServletRequest request, String name) {
        return request.getHeader(name);
    }
}
