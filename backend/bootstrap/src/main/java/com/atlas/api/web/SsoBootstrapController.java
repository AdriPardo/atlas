package com.atlas.api.web;

import com.atlas.application.auth.AuthenticateFromAuthentikUseCase;
import com.atlas.application.auth.AuthenticateFromAuthentikUseCase.AuthentikIdentity;
import com.atlas.domain.shared.UnauthorizedException;
import com.atlas.infrastructure.security.AuthentikHeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Browser-navigation SSO bootstrap. Traefik ForwardAuth injects {@code X-authentik-*} only on
 * document requests — not on SPA XHR — so prod mints JWT via full-page GET here, stores it in
 * {@code localStorage}, then redirects back to the SPA.
 */
@Controller
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SsoBootstrapController {

    /** Must match {@code frontend/src/shared/api/tokenStorage.ts}. */
    static final String TOKEN_STORAGE_KEY = "atlas.token";

    private static final String OUTPOST_START_PATH = "/outpost.goauthentik.io/start";

    private final AuthenticateFromAuthentikUseCase authenticateFromAuthentikUseCase;

    @GetMapping(value = "/sso/bootstrap", produces = MediaType.TEXT_HTML_VALUE)
    public void bootstrap(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(name = "returnTo", defaultValue = "/") String returnTo)
            throws IOException {
        String safeReturnTo = sanitizeReturnTo(returnTo);
        String username = header(request, AuthentikHeaderNames.USERNAME);

        if (username == null || username.isBlank()) {
            String bootstrapUrl = buildBootstrapUrl(request, safeReturnTo);
            String location = OUTPOST_START_PATH + "?rd=" + URLEncoder.encode(bootstrapUrl, StandardCharsets.UTF_8);
            response.sendRedirect(location);
            return;
        }

        try {
            var result = authenticateFromAuthentikUseCase.execute(new AuthentikIdentity(
                    username,
                    header(request, AuthentikHeaderNames.GROUPS),
                    header(request, AuthentikHeaderNames.EMAIL),
                    header(request, AuthentikHeaderNames.NAME),
                    header(request, AuthentikHeaderNames.UID)));
            writeBootstrapHtml(response, result.accessToken(), safeReturnTo);
        } catch (UnauthorizedException ex) {
            String bootstrapUrl = buildBootstrapUrl(request, safeReturnTo);
            String location = OUTPOST_START_PATH + "?rd=" + URLEncoder.encode(bootstrapUrl, StandardCharsets.UTF_8);
            response.sendRedirect(location);
        }
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

    private static String buildBootstrapUrl(HttpServletRequest request, String returnTo) {
        String scheme = forwarded(request, "X-Forwarded-Proto", request.getScheme());
        String host = forwarded(request, "X-Forwarded-Host", request.getHeader("Host"));
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        String encodedReturnTo = URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
        return scheme + "://" + host + "/api/v1/auth/sso/bootstrap?returnTo=" + encodedReturnTo;
    }

    private static String forwarded(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? fallback : value.split(",")[0].trim();
    }

    private static void writeBootstrapHtml(HttpServletResponse response, String accessToken, String returnTo)
            throws IOException {
        String html =
                """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Signing in…</title>
                </head>
                <body>
                  <p>Signing in to Atlas…</p>
                  <script>
                    sessionStorage.removeItem("atlas.sso.redirect");
                    localStorage.setItem(%s, %s);
                    window.location.replace(%s);
                  </script>
                </body>
                </html>
                """
                        .formatted(
                                toJsString(TOKEN_STORAGE_KEY),
                                toJsString(accessToken),
                                toJsString(returnTo));
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(html);
    }

    private static String header(HttpServletRequest request, String name) {
        return request.getHeader(name);
    }
}
