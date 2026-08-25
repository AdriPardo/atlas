package com.atlas.infrastructure.mail;

import com.atlas.application.port.out.ProjectSmtpProvisionerPort;
import com.atlas.domain.mail.ProjectMailNames;
import com.atlas.infrastructure.config.AtlasProperties;
import org.springframework.stereotype.Component;

@Component
public class PlatformSmtpProvisionerAdapter implements ProjectSmtpProvisionerPort {

    private final AtlasProperties properties;

    public PlatformSmtpProvisionerAdapter(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isConfigured() {
        return smtp().isConfigured();
    }

    @Override
    public String host() {
        return smtp().getHost().trim();
    }

    @Override
    public String hostForApps() {
        return smtp().resolveAppHost();
    }

    @Override
    public int port() {
        return smtp().getPort();
    }

    @Override
    public boolean tls() {
        return smtp().isTls();
    }

    @Override
    public boolean auth() {
        return smtp().isAuth();
    }

    @Override
    public String fromDomain() {
        return smtp().getFromDomain();
    }

    @Override
    public int dailySendLimitPerProject() {
        return Math.max(1, smtp().getDailySendLimitPerProject());
    }

    @Override
    public boolean autoInjectOnDeploy() {
        return smtp().isAutoInjectOnDeploy();
    }

    @Override
    public ProvisionResult provision(ProvisionRequest request) {
        AtlasProperties.AppSmtp cfg = smtp();
        String username = cfg.isAuth()
                ? ProjectMailNames.relayUsername(request.projectSlug())
                : "";
        String password = cfg.isAuth() ? request.relayPassword() : "";
        String from = ProjectMailNames.senderAddress(request.projectSlug(), cfg.getFromDomain());
        return new ProvisionResult(
                hostForApps(),
                port(),
                username,
                password,
                from,
                tls(),
                request.apiToken());
    }

    private AtlasProperties.AppSmtp smtp() {
        return properties.getAppSmtp();
    }
}
