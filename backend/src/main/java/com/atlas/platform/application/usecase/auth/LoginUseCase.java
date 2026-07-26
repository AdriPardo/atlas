package com.atlas.platform.application.usecase.auth;

import com.atlas.platform.domain.exception.UnauthorizedException;
import com.atlas.platform.domain.model.UserAccount;
import com.atlas.platform.domain.port.out.PasswordHasherPort;
import com.atlas.platform.domain.port.out.TokenProviderPort;
import com.atlas.platform.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenProviderPort tokenProvider;

    public LoginUseCase(
            UserRepositoryPort userRepository,
            PasswordHasherPort passwordHasher,
            TokenProviderPort tokenProvider) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public LoginResult execute(LoginCommand command) {
        UserAccount user = userRepository
                .findByUsername(command.username())
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = tokenProvider.createAccessToken(user);
        return new LoginResult(token, user.getId(), user.getUsername(), user.getRole());
    }
}
