package com.atlas.application.manifest;

import com.atlas.domain.manifest.ProjectManifest;
import com.atlas.domain.shared.DomainException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads {@code atlas.yml} / {@code atlas.project.yml} from a clone workspace (ADR-0014 phase B).
 */
public final class ProjectManifestLoader {

    public static final List<String> CANDIDATE_FILE_NAMES = List.of("atlas.yml", "atlas.project.yml");

    private final Yaml yaml;

    public ProjectManifestLoader() {
        this(new Yaml());
    }

    ProjectManifestLoader(Yaml yaml) {
        this.yaml = yaml;
    }

    /**
     * @return empty when no candidate file exists; throws when a file exists but is invalid
     */
    public Optional<ProjectManifest> load(Path workspace) {
        if (workspace == null) {
            throw new DomainException("Workspace path is required to load project manifest");
        }
        for (String name : CANDIDATE_FILE_NAMES) {
            Path path = workspace.resolve(name);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            return Optional.of(parseFile(path, name));
        }
        return Optional.empty();
    }

    private ProjectManifest parseFile(Path path, String fileName) {
        try (Reader reader = Files.newBufferedReader(path)) {
            Object loaded = yaml.load(reader);
            if (!(loaded instanceof Map<?, ?> root)) {
                throw new DomainException("Invalid " + fileName + ": expected a YAML mapping at root");
            }
            String apiVersion = stringField(root, "apiVersion");
            String kind = stringField(root, "kind");
            if (apiVersion == null || apiVersion.isBlank()) {
                throw new DomainException("Invalid " + fileName + ": apiVersion is required");
            }
            if (kind == null || kind.isBlank()) {
                throw new DomainException("Invalid " + fileName + ": kind is required");
            }
            if (!ProjectManifest.KIND_PROJECT.equals(kind.trim())) {
                throw new DomainException(
                        "Invalid " + fileName + ": kind must be " + ProjectManifest.KIND_PROJECT);
            }

            String runtimeKind = null;
            String composeFile = null;
            String migrateCommand = null;
            Object runtimeNode = root.get("runtime");
            if (runtimeNode instanceof Map<?, ?> runtime) {
                runtimeKind = stringField(runtime, "kind");
                composeFile = stringField(runtime, "composeFile");
                migrateCommand = stringField(runtime, "migrateCommand");
            } else if (runtimeNode != null) {
                throw new DomainException("Invalid " + fileName + ": runtime must be a mapping");
            }

            Boolean minify = null;
            Object buildNode = root.get("build");
            if (buildNode instanceof Map<?, ?> build) {
                minify = booleanField(build, "minify", fileName);
            } else if (buildNode != null) {
                throw new DomainException("Invalid " + fileName + ": build must be a mapping");
            }

            Boolean requireTls = null;
            Object exposureNode = root.get("exposure");
            if (exposureNode instanceof Map<?, ?> exposure) {
                requireTls = booleanField(exposure, "requireTls", fileName);
            } else if (exposureNode != null) {
                throw new DomainException("Invalid " + fileName + ": exposure must be a mapping");
            }

            return new ProjectManifest(
                    apiVersion, kind, runtimeKind, composeFile, migrateCommand, minify, requireTls, fileName);
        } catch (DomainException | IllegalArgumentException ex) {
            throw ex instanceof DomainException domain
                    ? domain
                    : new DomainException("Invalid " + fileName + ": " + ex.getMessage());
        } catch (IOException ex) {
            throw new DomainException("Failed to read " + fileName + ": " + ex.getMessage());
        } catch (Exception ex) {
            throw new DomainException("Failed to parse " + fileName + ": " + ex.getMessage());
        }
    }

    private static String stringField(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        return String.valueOf(value);
    }

    private static Boolean booleanField(Map<?, ?> map, String key, String fileName) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            String normalized = s.trim().toLowerCase(java.util.Locale.ROOT);
            if ("true".equals(normalized) || "false".equals(normalized)) {
                return Boolean.valueOf(normalized);
            }
        }
        throw new DomainException("Invalid " + fileName + ": " + key + " must be a boolean");
    }
}
