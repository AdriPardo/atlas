package com.atlas.api.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlas.domain.shared.UnauthorizedException;
import org.junit.jupiter.api.Test;

class SsoBootstrapControllerTest {

    @Test
    void sanitizeReturnToAllowsAppPaths() {
        assertThat(SsoBootstrapController.sanitizeReturnTo("/projects")).isEqualTo("/projects");
        assertThat(SsoBootstrapController.sanitizeReturnTo("/projects?tab=mail")).isEqualTo("/projects?tab=mail");
    }

    @Test
    void sanitizeReturnToRejectsExternalUrls() {
        assertThat(SsoBootstrapController.sanitizeReturnTo("https://evil.example")).isEqualTo("/");
        assertThat(SsoBootstrapController.sanitizeReturnTo("//evil.example/path")).isEqualTo("/");
        assertThat(SsoBootstrapController.sanitizeReturnTo("")).isEqualTo("/");
    }

    @Test
    void toJsStringEscapesQuotes() {
        assertThat(SsoBootstrapController.toJsString("a\"b")).isEqualTo("\"a\\\"b\"");
    }

    @Test
    void mapFailureCodeMapsKnownMessages() {
        assertThat(SsoBootstrapController.mapFailureCode(new UnauthorizedException("Authentik SSO is not enabled")))
                .isEqualTo("sso_disabled");
        assertThat(SsoBootstrapController.mapFailureCode(new UnauthorizedException("Missing Authentik identity")))
                .isEqualTo("identity_missing");
        assertThat(SsoBootstrapController.mapFailureCode(new UnauthorizedException("other")))
                .isEqualTo("mint_failed");
    }
}
