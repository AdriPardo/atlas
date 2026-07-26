package com.atlas.platform.api.rest;

import com.atlas.platform.api.dto.request.LoginRequest;
import com.atlas.platform.api.dto.response.AuthResponse;
import com.atlas.platform.api.dto.response.UserResponse;
import com.atlas.platform.api.mapper.ApiMapper;
import com.atlas.platform.application.usecase.auth.LoginCommand;
import com.atlas.platform.application.usecase.auth.LoginUseCase;
import com.atlas.platform.application.usecase.user.GetCurrentUserUseCase;
import com.atlas.platform.infrastructure.security.AtlasUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ApiMapper apiMapper;

    public AuthController(
            LoginUseCase loginUseCase,
            GetCurrentUserUseCase getCurrentUserUseCase,
            ApiMapper apiMapper) {
        this.loginUseCase = loginUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.apiMapper = apiMapper;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var result = loginUseCase.execute(new LoginCommand(request.username(), request.password()));
        return AuthResponse.bearer(
                result.accessToken(), result.userId(), result.username(), result.role());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AtlasUserDetails principal) {
        return apiMapper.toResponse(getCurrentUserUseCase.execute(principal.getId()));
    }
}
