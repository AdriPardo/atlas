package com.atlas.api.web;

import com.atlas.api.dto.common.PageResponse;
import com.atlas.api.dto.common.PageResponses;
import com.atlas.api.dto.request.CreateHostRequest;
import com.atlas.api.dto.request.UpdateHostRequest;
import com.atlas.api.dto.response.HostResponse;
import com.atlas.api.dto.response.JobResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.host.CreateHostUseCase;
import com.atlas.application.host.DeleteHostUseCase;
import com.atlas.application.host.GetHostUseCase;
import com.atlas.application.host.ListHostsUseCase;
import com.atlas.application.host.SyncHostUseCase;
import com.atlas.application.host.UpdateHostUseCase;
import com.atlas.application.shared.PageQuery;
import com.atlas.domain.host.ConnectionType;
import jakarta.validation.Valid;
import java.net.URI;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteHostUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
