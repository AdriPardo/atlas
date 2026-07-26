package com.atlas.application.auth;

import com.atlas.application.port.out.PasswordEncoderPort;
import com.atlas.application.port.out.TokenProviderPort;
import com.atlas.application.port.out.UserRepositoryPort;
import com.atlas.domain.shared.UnauthorizedException;
import com.atlas.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    @Transactional(readOnly = true)
    public AuthenticationResult execute(AuthenticateCommand command) {
        User user = userRepository
                .findByUsername(command.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = tokenProvider.generateToken(user);
        return new AuthenticationResult(token, "Bearer", tokenProvider.getExpirationSeconds());
    }

    public record AuthenticateCommand(String username, String password) {}
}
