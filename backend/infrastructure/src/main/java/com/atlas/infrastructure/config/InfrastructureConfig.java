package com.atlas.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ComponentScan(basePackages = {
    "com.atlas.application",
    "com.atlas.infrastructure"
})
@EntityScan(basePackages = "com.atlas.infrastructure.persistence.jpa.entity")
@EnableJpaRepositories(basePackages = "com.atlas.infrastructure.persistence.jpa.repository")
@EnableConfigurationProperties(AtlasProperties.class)
@EnableScheduling
public class InfrastructureConfig {}
