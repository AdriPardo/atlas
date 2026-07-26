package com.atlas.api.web;

import com.atlas.api.dto.common.PageResponse;
import com.atlas.api.dto.common.PageResponses;
import com.atlas.api.dto.request.CreateHostRequest;
import com.atlas.api.dto.request.UpdateHostRequest;
import com.atlas.api.dto.response.ContainerLogsResponse;
import com.atlas.api.dto.response.ContainerResponse;
import com.atlas.api.dto.response.HostResponse;
import com.atlas.api.dto.response.JobResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.host.CreateHostUseCase;
import com.atlas.application.host.DeleteHostUseCase;
import com.atlas.application.host.GetContainerLogsUseCase;
import com.atlas.application.host.GetHostUseCase;
import com.atlas.application.host.ListHostContainersUseCase;
import com.atlas.application.host.ListHostsUseCase;
import com.atlas.application.host.RestartContainerUseCase;
import com.atlas.application.host.SyncHostUseCase;
import com.atlas.application.host.UpdateHostUseCase;
import com.atlas.application.shared.GetObservabilitySettingsUseCase;
import com.atlas.application.shared.PageQuery;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.ContainerSnapshot;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hosts")
@RequiredArgsConstructor
public class HostController {

    private final CreateHostUseCase createHostUseCase;
    private final GetHostUseCase getHostUseCase;
    private final ListHostsUseCase listHostsUseCase;
    private final UpdateHostUseCase updateHostUseCase;
    private final DeleteHostUseCase deleteHostUseCase;
    private final SyncHostUseCase syncHostUseCase;
    private final ListHostContainersUseCase listHostContainersUseCase;
    private final GetContainerLogsUseCase getContainerLogsUseCase;
    private final RestartContainerUseCase restartContainerUseCase;
    private final GetObservabilitySettingsUseCase getObservabilitySettingsUseCase;
    private final ApiMapper apiMapper;

    @PostMapping
    public ResponseEntity<HostResponse> create(@Valid @RequestBody CreateHostRequest request) {
        var host = createHostUseCase.execute(new CreateHostUseCase.CreateHostCommand(
                request.hostname(),
                request.ip(),
                request.operatingSystem(),
                request.dockerVersion(),
                request.online(),
                request.connectionType() == null ? ConnectionType.LOCAL : request.connectionType(),
                request.sshUser(),
                request.sshPort(),
                request.sshPrivateKeySecretId()));
        return ResponseEntity.created(URI.create("/api/v1/hosts/" + host.getId()))
                .body(apiMapper.toHostResponse(host));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HostResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(apiMapper.toHostResponse(getHostUseCase.execute(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<HostResponse>> list(
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) Boolean online,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var result = listHostsUseCase.execute(hostname, online, new PageQuery(page, size, sort));
        return ResponseEntity.ok(PageResponses.from(result, apiMapper::toHostResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HostResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateHostRequest request) {
        var host = updateHostUseCase.execute(
                id,
                new UpdateHostUseCase.UpdateHostCommand(
                        request.hostname(),
                        request.ip(),
                        request.operatingSystem(),
                        request.dockerVersion(),
                        request.online(),
                        request.connectionType() == null ? ConnectionType.LOCAL : request.connectionType(),
                        request.sshUser(),
                        request.sshPort(),
                        request.sshPrivateKeySecretId()));
        return ResponseEntity.ok(apiMapper.toHostResponse(host));
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<JobResponse> sync(@PathVariable UUID id) {
        var job = syncHostUseCase.execute(id);
        return ResponseEntity.accepted().body(apiMapper.toJobResponse(job));
    }

    @GetMapping("/{id}/containers")
    public ResponseEntity<List<ContainerResponse>> listContainers(@PathVariable UUID id) {
        Host host = getHostUseCase.execute(id);
        List<ContainerSnapshot> containers = listHostContainersUseCase.execute(id);
        List<ContainerResponse> body = containers.stream()
                .map(c -> new ContainerResponse(
                        c.id(),
                        c.name(),
                        c.image(),
                        c.state(),
                        c.status(),
                        c.ports(),
                        c.labels(),
                        getObservabilitySettingsUseCase.containerLogsDeepLink(c.name(), host.getHostname())))
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}/containers/{containerRef}/logs")
    public ResponseEntity<ContainerLogsResponse> containerLogs(
            @PathVariable UUID id,
            @PathVariable String containerRef,
            @RequestParam(required = false) Integer tail) {
        String logs = getContainerLogsUseCase.execute(id, containerRef, tail);
        return ResponseEntity.ok(new ContainerLogsResponse(containerRef, logs));
    }

    @PostMapping("/{id}/containers/{containerRef}/restart")
    public ResponseEntity<Void> restartContainer(@PathVariable UUID id, @PathVariable String containerRef) {
        restartContainerUseCase.execute(id, containerRef);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteHostUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
