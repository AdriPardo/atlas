package com.atlas.domain.database;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Helpers to open a managed SQL console (pgweb) against an ADR-0015 connection URL.
 */
public final class DatabaseConsoleUrls {

    private DatabaseConsoleUrls() {}

    /**
     * Rewrites a {@code postgresql://} URL so lib/pq (pgweb) applies {@code search_path} to the
     * project schema. Strips JDBC-style {@code currentSchema}.
     */
    public static String withSearchPath(String connectionUrl, String schema) {
        if (connectionUrl == null || connectionUrl.isBlank()) {
            throw new IllegalArgumentException("connectionUrl is required");
        }
        if (schema == null || schema.isBlank()) {
            return connectionUrl;
        }
        String safeSchema = schema.trim().toLowerCase(Locale.ROOT);
        URI uri = URI.create(connectionUrl);
        String query = uri.getRawQuery();
        StringBuilder kept = new StringBuilder();
        if (query != null && !query.isBlank()) {
            for (String part : query.split("&")) {
                if (part.isBlank()) {
                    continue;
                }
                String key = part.split("=", 2)[0].toLowerCase(Locale.ROOT);
                if ("currentschema".equals(key) || "search_path".equals(key) || "options".equals(key)) {
                    continue;
                }
                if (!kept.isEmpty()) {
                    kept.append('&');
                }
                kept.append(part);
            }
        }
        String options = "options=-csearch_path%3D" + URLEncoder.encode(safeSchema, StandardCharsets.UTF_8);
        if (!kept.isEmpty()) {
            kept.append('&');
        }
        kept.append(options);

        String base = connectionUrl;
        int q = base.indexOf('?');
        if (q >= 0) {
            base = base.substring(0, q);
        }
        return base + "?" + kept;
    }

    public static String databaseName(String connectionUrl) {
        URI uri = URI.create(stripCredentialsForParse(connectionUrl));
        String path = uri.getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }
        String name = path.startsWith("/") ? path.substring(1) : path;
        int slash = name.indexOf('/');
        return slash >= 0 ? name.substring(0, slash) : name;
    }

    public static String serverHostPort(String connectionUrl) {
        URI uri = URI.create(stripCredentialsForParse(connectionUrl));
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return "";
        }
        int port = uri.getPort();
        return port > 0 ? host + ":" + port : host;
    }

    public static String username(String connectionUrl) {
        URI uri = URI.create(connectionUrl);
        String userInfo = uri.getRawUserInfo();
        if (userInfo == null || userInfo.isBlank()) {
            return "";
        }
        int colon = userInfo.indexOf(':');
        String user = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
        return URLDecoder.decode(user, StandardCharsets.UTF_8);
    }

    /** URI.create rejects some passwords; replace userinfo with placeholder for host/path parse. */
    private static String stripCredentialsForParse(String connectionUrl) {
        int scheme = connectionUrl.indexOf("://");
        if (scheme < 0) {
            return connectionUrl;
        }
        int at = connectionUrl.indexOf('@', scheme + 3);
        if (at < 0) {
            return connectionUrl;
        }
        return connectionUrl.substring(0, scheme + 3) + "x@" + connectionUrl.substring(at + 1);
    }
}
