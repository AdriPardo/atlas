package com.atlas.api.web;

import com.atlas.api.dto.request.LoginRequest;
import com.atlas.api.dto.response.LoginResponse;
import com.atlas.application.auth.AuthenticateFromAuthentikUseCase;
import com.atlas.application.auth.AuthenticateFromAuthentikUseCase.AuthentikIdentity;
import com.atlas.application.auth.AuthenticateUserUseCase;
import com.atlas.infrastructure.security.AuthentikHeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final AuthenticateFromAuthentikUseCase authenticateFromAuthentikUseCase;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authenticateUserUseCase.execute(
                new AuthenticateUserUseCase.AuthenticateCommand(request.username(), request.password()));
        return ResponseEntity.ok(new LoginResponse(result.accessToken(), result.tokenType(), result.expiresIn()));
    }

    /** Prod SSO mint — Traefik ForwardAuth injects X-authentik-* on fetch (same-origin, credentials). */
    @GetMapping("/sso")
    public ResponseEntity<LoginResponse> ssoGet(HttpServletRequest request) {
        return mintSso(request);
    }

    @PostMapping("/sso")
    public ResponseEntity<LoginResponse> ssoPost(HttpServletRequest request) {
        return mintSso(request);
    }

    private ResponseEntity<LoginResponse> mintSso(HttpServletRequest request) {
        var result = authenticateFromAuthentikUseCase.execute(new AuthentikIdentity(
                header(request, AuthentikHeaderNames.USERNAME),
                header(request, AuthentikHeaderNames.GROUPS),
                header(request, AuthentikHeaderNames.EMAIL),
                header(request, AuthentikHeaderNames.NAME),
                header(request, AuthentikHeaderNames.UID)));
        return ResponseEntity.ok(new LoginResponse(result.accessToken(), result.tokenType(), result.expiresIn()));
    }

    private static String header(HttpServletRequest request, String name) {
        return request.getHeader(name);
    }
}
