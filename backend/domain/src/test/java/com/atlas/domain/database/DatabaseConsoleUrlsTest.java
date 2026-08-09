package com.atlas.domain.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DatabaseConsoleUrlsTest {

    @Test
    void withSearchPathReplacesCurrentSchema() {
        String in =
                "postgresql://app_demo_t_abcd:s3cret@postgres:5432/apps?currentSchema=app_demo";
        String out = DatabaseConsoleUrls.withSearchPath(in, "app_demo");
        assertTrue(out.startsWith("postgresql://app_demo_t_abcd:s3cret@postgres:5432/apps?"));
        assertTrue(out.contains("options=-csearch_path%3Dapp_demo"));
        assertFalse(out.contains("currentSchema"));
    }

    @Test
    void parsesDatabaseAndServer() {
        String url = "postgresql://u:p@postgres:5432/apps?currentSchema=app_x";
        assertEquals("apps", DatabaseConsoleUrls.databaseName(url));
        assertEquals("postgres:5432", DatabaseConsoleUrls.serverHostPort(url));
        assertEquals("u", DatabaseConsoleUrls.username(url));
    }
}
