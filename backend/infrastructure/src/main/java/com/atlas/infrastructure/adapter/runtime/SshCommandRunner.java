package com.atlas.infrastructure.adapter.runtime;

import com.atlas.domain.shared.DomainException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.io.resource.PathResource;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.springframework.stereotype.Component;

@Component
public class SshCommandRunner {

    public String run(
            String host,
            int port,
            String username,
            String privateKeyPem,
            String command,
            Consumer<String> logSink) {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            try (ClientSession session = client
                    .connect(username, host, port)
                    .verify(Duration.ofSeconds(20))
                    .getSession()) {
                loadKey(session, privateKeyPem);
                session.auth().verify(Duration.ofSeconds(20));

                try (ClientChannel channel = session.createExecChannel(command)) {
                    java.io.ByteArrayOutputStream stdout = new java.io.ByteArrayOutputStream();
                    java.io.ByteArrayOutputStream stderr = new java.io.ByteArrayOutputStream();
                    channel.setOut(stdout);
                    channel.setErr(stderr);
                    channel.open().verify(Duration.ofSeconds(20));
                    channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.MINUTES.toMillis(30));
                    String out = stdout.toString(StandardCharsets.UTF_8);
                    String err = stderr.toString(StandardCharsets.UTF_8);
                    if (logSink != null) {
                        out.lines().forEach(logSink);
                        err.lines().forEach(line -> logSink.accept("[stderr] " + line));
                    }
                    Integer status = channel.getExitStatus();
                    if (status != null && status != 0) {
                        throw new DomainException("Remote command failed (" + status + "): " + command);
                    }
                    return out + err;
                }
            }
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("SSH command failed: " + ex.getMessage());
        }
    }

    public void uploadDirectory(
            String host,
            int port,
            String username,
            String privateKeyPem,
            Path localDirectory,
            String remoteDirectory,
            Consumer<String> logSink) {
        try {
            Path tar = Files.createTempFile("atlas-workspace-", ".tar");
            try {
                new ProcessCommandRunner()
                        .run(
                                List.of("tar", "-cf", tar.toString(), "-C", localDirectory.toString(), "."),
                                null,
                                line -> {});
                byte[] bytes = Files.readAllBytes(tar);
                String remoteCmd = "mkdir -p "
                        + shellQuote(remoteDirectory)
                        + " && tar -xf - -C "
                        + shellQuote(remoteDirectory);
                logSink.accept("Uploading workspace archive (" + bytes.length + " bytes)");
                try (SshClient client = SshClient.setUpDefaultClient()) {
                    client.start();
                    try (ClientSession session = client
                            .connect(username, host, port)
                            .verify(Duration.ofSeconds(20))
                            .getSession()) {
                        loadKey(session, privateKeyPem);
                        session.auth().verify(Duration.ofSeconds(20));
                        try (ClientChannel channel = session.createExecChannel(remoteCmd)) {
                            channel.setIn(new ByteArrayInputStream(bytes));
                            java.io.ByteArrayOutputStream stderr = new java.io.ByteArrayOutputStream();
                            channel.setErr(stderr);
                            channel.open().verify(Duration.ofSeconds(20));
                            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.MINUTES.toMillis(30));
                            Integer status = channel.getExitStatus();
                            if (status != null && status != 0) {
                                throw new DomainException(
                                        "Remote upload failed (" + status + "): " + stderr.toString(StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
            } finally {
                Files.deleteIfExists(tar);
            }
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("Workspace upload failed: " + ex.getMessage());
        }
    }

    private static void loadKey(ClientSession session, String privateKeyPem) throws Exception {
        Path keyFile = Files.createTempFile("atlas-ssh-", ".pem");
        try {
            Files.writeString(keyFile, privateKeyPem, StandardCharsets.UTF_8);
            Iterable<KeyPair> keys = SecurityUtils.getKeyPairResourceParser()
                    .loadKeyPairs(null, new PathResource(keyFile), FilePasswordProvider.EMPTY);
            for (KeyPair keyPair : keys) {
                session.addPublicKeyIdentity(keyPair);
            }
        } finally {
            Files.deleteIfExists(keyFile);
        }
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
