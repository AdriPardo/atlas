package com.atlas.platform.application.usecase.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.exception.ConflictException;
import com.atlas.platform.domain.model.Application;
import com.atlas.platform.domain.model.ApplicationStatus;
import com.atlas.platform.domain.port.out.ApplicationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateApplicationUseCaseTest {

    @Mock
    private ApplicationRepositoryPort applicationRepository;

    private CreateApplicationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateApplicationUseCase(applicationRepository);
    }

    @Test
    void createsApplicationInDraftStatus() {
        when(applicationRepository.existsByName(
                        eq(InstallationContext.DEFAULT_INSTALLATION_ID), eq("billing"), isNull()))
                .thenReturn(false);
        when(applicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Application created = useCase.execute(new CreateApplicationCommand(
                "billing", "Billing service", "https://git.example/billing", "main", "compose.yml", "billing.local"));

        assertThat(created.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(created.getName()).isEqualTo("billing");
        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());
        assertThat(captor.getValue().getInstallationId())
                .isEqualTo(InstallationContext.DEFAULT_INSTALLATION_ID);
    }

    @Test
    void rejectsDuplicateName() {
        when(applicationRepository.existsByName(
                        eq(InstallationContext.DEFAULT_INSTALLATION_ID), eq("billing"), isNull()))
                .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new CreateApplicationCommand(
                        "billing", null, "https://git.example/billing", "main", "compose.yml", null)))
                .isInstanceOf(ConflictException.class);
    }
}
