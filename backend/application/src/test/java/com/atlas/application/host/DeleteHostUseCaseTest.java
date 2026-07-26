package com.atlas.application.host;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.ConflictException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteHostUseCaseTest {

    @Mock
    private HostRepositoryPort hostRepository;

    @Mock
    private DeploymentRepositoryPort deploymentRepository;

    @InjectMocks
    private DeleteHostUseCase useCase;

    @Test
    void rejectsDeleteWhenDeploymentsExist() {
        UUID id = UUID.randomUUID();
        when(hostRepository.findById(id)).thenReturn(Optional.of(Host.create("h1", "10.0.0.1", "linux", "26", true)));
        when(deploymentRepository.existsByHostId(id)).thenReturn(true);

        assertThrows(ConflictException.class, () -> useCase.execute(id));
    }
}
