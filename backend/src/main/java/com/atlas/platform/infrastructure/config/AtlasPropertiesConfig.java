package com.atlas.platform.infrastructure.config;

import com.atlas.platform.infrastructure.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AtlasPropertiesConfig {}
