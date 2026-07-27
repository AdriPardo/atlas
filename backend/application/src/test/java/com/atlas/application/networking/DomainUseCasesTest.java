package com.atlas.application.networking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.DnsProviderPort;
import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.port.out.TraefikMetadataPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.networking.Domain;
import com.atlas.domain.networking.DomainStatus;
import com.atlas.domain.project.Project;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DomainUseCasesTest {

    @Mock
    private DomainRepositoryPort domainRepository;

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private ServiceRepositoryPort serviceRepository;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private RecordAuditUseCase recordAuditUseCase;

    @Mock
    private DnsProviderPort dnsProviderPort;

    @Mock
    private TraefikMetadataPort traefikMetadataPort;

    @InjectMocks
    private ManageDomainUseCase manageDomainUseCase;

    @InjectMocks
    private VerifyDomainUseCase verifyDomainUseCase;

    @InjectMocks
    private GetDomainTraefikMetadataUseCase getDomainTraefikMetadataUseCase;

    @Test
    void createRequiresWriteAndPersists() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(org.mockito.Mockito.mock(Project.class)));
        when(domainRepository.existsByProjectIdAndHostnameIgnoreCase(projectId, "app.example.com"))
                .thenReturn(false);
        when(domainRepository.save(any(Domain.class))).thenAnswer(inv -> inv.getArgument(0));

        Domain created = manageDomainUseCase.create(projectId, "app.example.com", null);

        assertEquals("app.example.com", created.getHostname());
        assertEquals(DomainStatus.PENDING_DNS, created.getStatus());
        verify(authorizationService).require(projectId, ProjectPermission.WRITE);
        verify(recordAuditUseCase).execute(eq("DOMAIN_CREATE"), eq("domain"), eq(created.getId()), anyString());
    }

    @Test
    void createRejectsDuplicateHostname() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(org.mockito.Mockito.mock(Project.class)));
        when(domainRepository.existsByProjectIdAndHostnameIgnoreCase(projectId, "dup.example.com"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> manageDomainUseCase.create(projectId, "dup.example.com", null));
    }

    @Test
    void listRequiresRead() {
        UUID projectId = UUID.randomUUID();
        when(domainRepository.findByProjectId(projectId)).thenReturn(List.of());
        assertTrue(manageDomainUseCase.list(projectId).isEmpty());
        verify(authorizationService).require(projectId, ProjectPermission.READ);
    }

    @Test
    void verifyMarksActiveAndAudits() {
        Domain domain = Domain.create(UUID.randomUUID(), "verify.example.com", null);
        when(domainRepository.findById(domain.getId())).thenReturn(Optional.of(domain));
        when(dnsProviderPort.syncChallenge(domain))
                .thenReturn(new DnsProviderPort.DnsSyncResult(false, "stub"));
        when(domainRepository.save(any(Domain.class))).thenAnswer(inv -> inv.getArgument(0));

        Domain verified = verifyDomainUseCase.execute(domain.getId());

        assertEquals(DomainStatus.ACTIVE, verified.getStatus());
        assertEquals("letsencrypt-stub", verified.getCertificateIssuer());
        verify(authorizationService).require(domain.getProjectId(), ProjectPermission.WRITE);
        verify(recordAuditUseCase).execute(eq("DOMAIN_VERIFY"), eq("domain"), eq(domain.getId()), anyString());
    }

    @Test
    void getMissingDomainThrows() {
        UUID id = UUID.randomUUID();
        when(domainRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> manageDomainUseCase.get(id));
    }

    @Test
    void traefikMetadataRequiresRead() {
        Domain domain = Domain.create(UUID.randomUUID(), "edge.example.com", null);
        when(domainRepository.findById(domain.getId())).thenReturn(Optional.of(domain));
        TraefikMetadataPort.TraefikRouteMetadata expected =
                TraefikMetadataPort.TraefikRouteMetadata.of("atlas-edge-example-com", "edge.example.com", "letsencrypt", 80);
        when(traefikMetadataPort.metadataFor(eq(domain), eq("atlas-edge-example-com"))).thenReturn(expected);

        TraefikMetadataPort.TraefikRouteMetadata metadata =
                getDomainTraefikMetadataUseCase.execute(domain.getId());

        assertEquals("Host(`edge.example.com`)", metadata.rule());
        assertTrue(metadata.labels().containsKey("traefik.enable"));
        verify(authorizationService).require(domain.getProjectId(), ProjectPermission.READ);
    }
}
