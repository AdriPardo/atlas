package com.atlas.domain.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.atlas.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

class ProjectDatabaseNamesTest {

    @Test
    void schemaAndRoleFromSlug() {
        assertEquals("app_reelpath", ProjectDatabaseNames.schemaName("reelpath"));
        assertEquals("app_reelpath_migrator", ProjectDatabaseNames.migratorRole("reelpath"));
        assertEquals("app_my_app", ProjectDatabaseNames.schemaName("my-app"));
        assertEquals("app_my_app_migrator", ProjectDatabaseNames.migratorRole("my-app"));
    }

    @Test
    void rejectsControlPlaneDatabase() {
        assertThrows(DomainException.class, () -> ProjectDatabaseNames.rejectControlPlaneDatabase("atlas"));
        assertThrows(DomainException.class, () -> ProjectDatabaseNames.rejectControlPlaneDatabase("ATLAS"));
        ProjectDatabaseNames.rejectControlPlaneDatabase("apps");
    }
}
