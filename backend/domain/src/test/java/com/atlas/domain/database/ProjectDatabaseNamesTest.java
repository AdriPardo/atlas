package com.atlas.domain.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

class ProjectDatabaseNamesTest {

    @Test
    void namingHelpers() {
        assertEquals("app_reelpath", ProjectDatabaseNames.schemaName("reelpath"));
        assertEquals("app_reelpath_migrator", ProjectDatabaseNames.migratorRole("reelpath"));
        assertEquals("app_reelpath_ro", ProjectDatabaseNames.readOnlyRole("reelpath"));
        assertEquals("app_my_app", ProjectDatabaseNames.schemaName("my-app"));
        assertEquals("app_my_app_migrator", ProjectDatabaseNames.migratorRole("my-app"));
        assertEquals("app_my_app_ro", ProjectDatabaseNames.readOnlyRole("my-app"));
        assertEquals("app_reelpath_t_", ProjectDatabaseNames.temporaryCredentialRolePrefix("reelpath"));
        assertEquals(
                "app_reelpath_t_abcd1234",
                ProjectDatabaseNames.temporaryCredentialRole("reelpath", "abcd1234"));
    }

    @Test
    void rejectsControlPlaneDatabase() {
        assertThrows(DomainException.class, () -> ProjectDatabaseNames.rejectControlPlaneDatabase("atlas"));
        assertThrows(DomainException.class, () -> ProjectDatabaseNames.rejectControlPlaneDatabase("ATLAS"));
        ProjectDatabaseNames.rejectControlPlaneDatabase("apps");
    }

    @Test
    void clampsTtl() {
        assertEquals(60, ProjectDatabaseNames.clampTtlMinutes(null));
        assertEquals(30, ProjectDatabaseNames.clampTtlMinutes(30));
        assertThrows(DomainException.class, () -> ProjectDatabaseNames.clampTtlMinutes(1));
        assertThrows(DomainException.class, () -> ProjectDatabaseNames.clampTtlMinutes(10_000));
    }

    @Test
    void profileWire() {
        assertEquals(DatabaseAccessProfile.READ, DatabaseAccessProfile.fromWire(null));
        assertEquals(DatabaseAccessProfile.READ, DatabaseAccessProfile.fromWire("db.read"));
        assertEquals(DatabaseAccessProfile.MIGRATE, DatabaseAccessProfile.fromWire("db.migrate"));
        assertEquals(DatabaseAccessProfile.ADMIN, DatabaseAccessProfile.fromWire("ADMIN"));
        assertThrows(DomainException.class, () -> DatabaseAccessProfile.fromWire("db.nope"));
        assertTrue(DatabaseAccessProfile.READ.wire().startsWith("db."));
    }
}
