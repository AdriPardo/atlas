package com.atlas.infrastructure.adapter.runtime;

import com.atlas.application.port.out.HostCommandPort;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.shared.DomainException;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Executes {@code runtime.migrateCommand} (and similar hooks) on LOCAL or SSH hosts.
 */
@Component
@ConditionalOnProperty(name = "atlas.adapters.real-enabled", havingValue = "true", matchIfMissing = true)
public class ProcessHostCommandAdapter implements HostCommandPort {

    private final ProcessCommandRunner processCommandRunner;
    private final SshCommandRunner sshCommandRunner;

    public ProcessHostCommandAdapter(ProcessCommandRunner processCommandRunner, SshCommandRunner sshCommandRunner) {
        this.processCommandRunner = processCommandRunner;
        this.sshCommandRunner = sshCommandRunner;
    }

    @Override
    public void run(HostCommand command) {
        String shell = command.shellCommand();
        if (shell == null || shell.isBlank()) {
            throw new DomainException("Host command is blank");
        }

        if (command.host().getConnectionType() == ConnectionType.LOCAL) {
            command.logSink().accept("Running migrateCommand (local): " + shell);
            processCommandRunner.run(List.of("sh", "-c", shell), command.workingDirectory(), command.logSink());
            return;
        }

        String key = command.sshPrivateKeyPem()
                .orElseThrow(() -> new DomainException("SSH private key required for remote migrateCommand"));
        String user = command.host().getSshUser() == null ? "root" : command.host().getSshUser();
        String remoteDir = "/var/lib/atlas/workspaces/" + command.workingDirectory().getFileName();
        // Workspace already uploaded by compose apply on the same deploy; re-run in that dir.
        String remoteCmd = "cd " + shellQuote(remoteDir) + " && " + shell;
        command.logSink().accept("Running migrateCommand (remote): " + remoteCmd);
        sshCommandRunner.run(
                command.host().getIp(),
                command.host().getSshPort(),
                user,
                key,
                remoteCmd,
                command.logSink());
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
