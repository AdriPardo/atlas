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
        assertTrue(manifest.getEnvFromSecrets().isEmpty());
    }

    @Test
    void envFromSecretRefMapsDbUrlToDatabaseUrl() {
        EnvFromSecretRef ref = EnvFromSecretRef.of("db.url");
        assertEquals("DATABASE_URL", ref.resolveEnvKey());
        assertEquals("CUSTOM", new EnvFromSecretRef("db.url", "CUSTOM").resolveEnvKey());
    }

    @Test
    void envFromSecretRefMapsPlatformAiSecrets() {
        assertEquals("OPENAI_API_KEY", EnvFromSecretRef.of("ai.openai").resolveEnvKey());
        assertEquals("OPENAI_BASE_URL", EnvFromSecretRef.of("ai.openai.base_url").resolveEnvKey());
        assertEquals("ELEVENLABS_API_KEY", EnvFromSecretRef.of("ai.elevenlabs").resolveEnvKey());
        assertEquals("DEEPSEEK_API_KEY", EnvFromSecretRef.of("ai.deepseek").resolveEnvKey());
        assertEquals("AI_PROVIDER", EnvFromSecretRef.of("ai.provider").resolveEnvKey());
        assertEquals("AI_API_KEY", EnvFromSecretRef.of("ai.api_key").resolveEnvKey());
        assertEquals("AI_BASE_URL", EnvFromSecretRef.of("ai.base_url").resolveEnvKey());
    }
}
