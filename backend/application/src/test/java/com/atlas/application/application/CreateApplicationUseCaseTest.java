package com.atlas.application.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.domain.application.Application;
import com.atlas.domain.shared.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateApplicationUseCaseTest {

    @Mock
    private ApplicationRepositoryPort applicationRepository;

    @InjectMocks
    private CreateApplicationUseCase useCase;

    @Test
    void createsApplicationWhenNameIsUnique() {
        when(applicationRepository.existsByName("billing")).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application result = useCase.execute(new CreateApplicationUseCase.CreateApplicationCommand(
                "billing",
                "Billing",
                "https://git.example/billing.git",
                "main",
                "./docker-compose.yml",
                "billing.local"));

        assertEquals("billing", result.getName());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void rejectsDuplicateName() {
        when(applicationRepository.existsByName("billing")).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> useCase.execute(new CreateApplicationUseCase.CreateApplicationCommand(
                        "billing",
                        "Billing",
                        "https://git.example/billing.git",
                        "main",
                        "./docker-compose.yml",
                        "billing.local")));
    }
}
