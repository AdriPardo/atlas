package com.atlas.domain.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProjectManifestTest {

    @Test
    void synthesizesMinimalComposeManifestFromPath() {
        ProjectManifest manifest = ProjectManifest.synthesizeFromComposePath("docker-compose.yml");

        assertTrue(manifest.isSynthesized());
        assertTrue(manifest.isComposeCompatible());
        assertEquals("compose", manifest.getRuntimeKind().orElseThrow());
        assertEquals("docker-compose.yml", manifest.getComposeFile().orElseThrow());
        assertEquals(ProjectManifest.API_VERSION_V1_ALPHA1, manifest.getApiVersion());
        assertEquals(ProjectManifest.KIND_PROJECT, manifest.getKind());
        assertTrue(manifest.isMinifyEnabled());
        assertTrue(manifest.isRequireTlsEnabled());
    }
}
