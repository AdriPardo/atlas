package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.RuntimeCapability;
import com.atlas.infrastructure.persistence.jpa.entity.HostJpaEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class HostJpaMapper {

    private static final TypeReference<List<String>> TAG_LIST = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public HostJpaMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Host toDomain(HostJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Host.rehydrate(
                entity.getId(),
                entity.getHostname(),
                entity.getIp(),
                entity.getOperatingSystem(),
                entity.getDockerVersion(),
                entity.isOnline(),
                entity.getConnectionType() == null ? ConnectionType.LOCAL : entity.getConnectionType(),
                entity.getSshUser(),
                entity.getSshPort() == 0 ? 22 : entity.getSshPort(),
                entity.getSshPrivateKeySecretId(),
                parseCapabilities(entity.getRuntimeCapabilities()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public HostJpaEntity toEntity(Host domain) {
        if (domain == null) {
            return null;
        }
        HostJpaEntity entity = new HostJpaEntity();
        entity.setId(domain.getId());
        entity.setHostname(domain.getHostname());
        entity.setIp(domain.getIp());
        entity.setOperatingSystem(domain.getOperatingSystem());
        entity.setDockerVersion(domain.getDockerVersion());
        entity.setOnline(domain.isOnline());
        entity.setConnectionType(domain.getConnectionType());
        entity.setSshUser(domain.getSshUser());
        entity.setSshPort(domain.getSshPort());
        entity.setSshPrivateKeySecretId(domain.getSshPrivateKeySecretId());
        entity.setRuntimeCapabilities(writeCapabilities(domain.runtimeCapabilities()));
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private Set<RuntimeCapability> parseCapabilities(String json) {
        if (json == null || json.isBlank()) {
            return Set.of(RuntimeCapability.COMPOSE);
        }
        try {
            List<String> tags = objectMapper.readValue(json, TAG_LIST);
            if (tags == null || tags.isEmpty()) {
                return Set.of(RuntimeCapability.COMPOSE);
            }
            Set<RuntimeCapability> caps = new LinkedHashSet<>();
            for (String tag : tags) {
                if (tag == null || tag.isBlank()) {
                    continue;
                }
                caps.add(RuntimeCapability.fromTag(tag));
            }
            return caps.isEmpty() ? Set.of(RuntimeCapability.COMPOSE) : Set.copyOf(caps);
        } catch (Exception ex) {
            return Set.of(RuntimeCapability.COMPOSE);
        }
    }

    private String writeCapabilities(Set<RuntimeCapability> capabilities) {
        List<String> tags = capabilities.stream().map(RuntimeCapability::tag).collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception ex) {
            return "[\"compose\"]";
        }
    }
}
