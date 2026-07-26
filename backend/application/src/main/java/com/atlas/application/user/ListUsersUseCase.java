package com.atlas.application.user;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.UserRepositoryPort;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {

    private final UserRepositoryPort userRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<User> execute() {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can list users");
        }
        return userRepository.findAll();
    }
}
