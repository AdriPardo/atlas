package com.atlas.infrastructure.adapter.runtime;

import com.atlas.domain.shared.DomainException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class ProcessCommandRunner {

    public String run(List<String> command, Path workingDirectory, Consumer<String> logSink) {
        return run(command, workingDirectory, Map.of(), logSink);
    }

    public String run(
            List<String> command, Path workingDirectory, Map<String, String> extraEnv, Consumer<String> logSink) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            if (workingDirectory != null) {
                builder.directory(workingDirectory.toFile());
            }
            if (extraEnv != null && !extraEnv.isEmpty()) {
                builder.environment().putAll(extraEnv);
            }
            builder.redirectErrorStream(true);
            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                    if (logSink != null) {
                        logSink.accept(line);
                    }
                }
            }
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new DomainException("Command timed out: " + String.join(" ", command));
            }
            if (process.exitValue() != 0) {
                throw new DomainException(
                        "Command failed (" + process.exitValue() + "): " + String.join(" ", command));
            }
            return output.toString();
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("Command execution failed: " + ex.getMessage());
        }
    }
}
