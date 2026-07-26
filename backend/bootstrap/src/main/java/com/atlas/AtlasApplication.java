package com.atlas;

import com.atlas.infrastructure.config.InfrastructureConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "com.atlas.api")
@Import(InfrastructureConfig.class)
public class AtlasApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtlasApplication.class, args);
    }
}
