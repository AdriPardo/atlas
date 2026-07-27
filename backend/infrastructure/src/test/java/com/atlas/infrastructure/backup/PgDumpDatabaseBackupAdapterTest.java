package com.atlas.infrastructure.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.atlas.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

class PgDumpDatabaseBackupAdapterTest {

    @Test
    void parsesStandardJdbcUrl() {
        var target = PgDumpDatabaseBackupAdapter.JdbcPostgresTarget.parse(
                "jdbc:postgresql://postgres:5432/atlas");
        assertEquals("postgres", target.host());
        assertEquals(5432, target.port());
        assertEquals("atlas", target.database());
    }

    @Test
    void parsesUrlWithQueryParamsAndDefaultPort() {
        var target = PgDumpDatabaseBackupAdapter.JdbcPostgresTarget.parse(
                "jdbc:postgresql://db.example/atlas?sslmode=require");
        assertEquals("db.example", target.host());
        assertEquals(5432, target.port());
        assertEquals("atlas", target.database());
    }

    @Test
    void rejectsNonPostgresUrl() {
        assertThrows(
                DomainException.class,
                () -> PgDumpDatabaseBackupAdapter.JdbcPostgresTarget.parse("jdbc:h2:mem:test"));
    }
}
