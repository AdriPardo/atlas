package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.host.Host;
import com.atlas.infrastructure.persistence.jpa.entity.HostJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class HostJpaMapper {

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
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
