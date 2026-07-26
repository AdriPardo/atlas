package com.atlas.platform.application.usecase.user;

import com.atlas.platform.domain.exception.NotFoundException;
import com.atlas.platform.domain.model.UserAccount;
import com.atlas.platform.domain.port.out.UserRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentUserUseCase {

    private final UserRepositoryPort userRepository;

    public GetCurrentUserUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserAccount execute(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
