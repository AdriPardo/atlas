package com.atlas.platform.api.rest;

import com.atlas.platform.api.dto.response.HostResponse;
import com.atlas.platform.api.dto.response.PageResponse;
import com.atlas.platform.api.mapper.ApiMapper;
import com.atlas.platform.application.usecase.host.GetHostUseCase;
import com.atlas.platform.application.usecase.host.ListHostsUseCase;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hosts")
public class HostController {

    private final ListHostsUseCase listHostsUseCase;
    private final GetHostUseCase getHostUseCase;
    private final ApiMapper apiMapper;

    public HostController(
            ListHostsUseCase listHostsUseCase, GetHostUseCase getHostUseCase, ApiMapper apiMapper) {
        this.listHostsUseCase = listHostsUseCase;
        this.getHostUseCase = getHostUseCase;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public PageResponse<HostResponse> list(
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) Boolean online,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        boolean ascending = "asc".equalsIgnoreCase(sortDir);
        return apiMapper.toPage(
                listHostsUseCase.execute(hostname, online, page, size, sortBy, ascending),
                apiMapper::toResponse);
    }

    @GetMapping("/{id}")
    public HostResponse get(@PathVariable UUID id) {
        return apiMapper.toResponse(getHostUseCase.execute(id));
    }
}
