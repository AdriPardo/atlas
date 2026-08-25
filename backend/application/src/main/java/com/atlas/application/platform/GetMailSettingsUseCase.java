package com.atlas.application.platform;

import com.atlas.application.port.out.ProjectSmtpProvisionerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMailSettingsUseCase {

    public record MailSettings(
            boolean configured,
            String host,
            int port,
            String fromDomain,
            boolean tls,
            boolean auth,
            int dailySendLimitPerProject) {}

    private final ProjectSmtpProvisionerPort provisioner;

    @Transactional(readOnly = true)
    public MailSettings execute() {
        if (!provisioner.isConfigured()) {
            return new MailSettings(false, null, 0, provisioner.fromDomain(), false, false, 0);
        }
        return new MailSettings(
                true,
                provisioner.host(),
                provisioner.port(),
                provisioner.fromDomain(),
                provisioner.tls(),
                provisioner.auth(),
                provisioner.dailySendLimitPerProject());
    }
}
