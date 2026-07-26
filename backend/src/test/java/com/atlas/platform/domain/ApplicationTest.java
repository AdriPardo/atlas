package com.atlas.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.model.Application;
import com.atlas.platform.domain.model.ApplicationStatus;
import org.junit.jupiter.api.Test;

class ApplicationTest {

    @Test
    void createDefaultsToDraft() {
        Application app = Application.create(
                InstallationContext.DEFAULT_INSTALLATION_ID,
                "api",
                "desc",
                "https://example.com/repo.git",
                "main",
                "docker-compose.yml",
                "api.local");
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(app.getId()).isNotNull();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Application.create(
                        InstallationContext.DEFAULT_INSTALLATION_ID,
                        " ",
                        null,
                        "https://example.com/repo.git",
                        "main",
                        "docker-compose.yml",
                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
