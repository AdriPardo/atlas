package com.atlas.application.port.out;

import java.nio.file.Path;

public interface DatabaseBackupPort {

    /**
     * Creates a logical Postgres dump under {@code targetDir}.
     *
     * @return absolute path of the created dump file
     */
    Path dumpTo(Path targetDir);
}
