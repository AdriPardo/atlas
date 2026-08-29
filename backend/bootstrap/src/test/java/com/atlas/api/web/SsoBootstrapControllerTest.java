package com.atlas.api.web;

import static org.assertj.core.api.Assertions.assertThat;

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
}
