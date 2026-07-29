package com.atlas.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String API_VERSION = "v1";
    public static final String APPLICATIONS_SUNSET = "Wed, 01 Jul 2027 00:00:00 GMT";

    @Bean
    public OpenAPI atlasOpenApi() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Atlas API")
                        .description(
                                """
                                Operations platform for self-hosted applications.

                                Base path: `/api/v1`. Authenticate with `Authorization: Bearer <jwt>` \
                                from `POST /api/v1/auth/login` or SSO.

                                **Deprecations**
                                - `/api/v1/applications` is a deprecated alias for Project + default Service. \
                                Prefer `/api/v1/projects` and `/api/v1/services`. Responses include \
                                `Deprecation: true`, `Sunset: %s`, and `Link` successor headers. \
                                Alias remains until Sunset; see `docs/api/deprecations.md`.

                                Published contract snapshot: `docs/api/openapi.json` (regenerate with \
                                `-Datlas.writeOpenApi=true` on `OpenApiContractIntegrationTest`).
                                """
                                        .formatted(APPLICATIONS_SUNSET))
                        .version(API_VERSION)
                        .contact(new Contact().name("Atlas").url("https://github.com/AdriPardo/atlas"))
                        .license(new License().name("Proprietary")))
                .externalDocs(new ExternalDocumentation()
                        .description("API conventions & deprecations")
                        .url("https://github.com/AdriPardo/atlas/blob/master/docs/api/conventions.md"))
                .addServersItem(new Server().url("/").description("Current host"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(
                                schemeName,
                                new SecurityScheme()
                                        .name(schemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
