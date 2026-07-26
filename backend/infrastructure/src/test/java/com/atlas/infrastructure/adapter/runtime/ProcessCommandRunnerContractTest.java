package com.atlas.infrastructure.adapter.runtime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessCommandRunnerContractTest {

    private final ProcessCommandRunner runner = new ProcessCommandRunner();

    @Test
    void capturesStdoutFromSuccessfulCommand() {
        List<String> lines = new ArrayList<>();
        String output = runner.run(List.of("echo", "atlas-ok"), null, lines::add);
        assertTrue(output.contains("atlas-ok"));
        assertTrue(lines.stream().anyMatch(l -> l.contains("atlas-ok")));
    }

    @Test
    void failsOnNonZeroExit() {
        assertThrows(
                com.atlas.domain.shared.DomainException.class,
                () -> runner.run(List.of("sh", "-c", "exit 7"), null, line -> {}));
    }
}
