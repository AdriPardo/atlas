package com.atlas.application.user;

import com.atlas.application.port.out.UserRepositoryPort;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.user.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    public User execute(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
