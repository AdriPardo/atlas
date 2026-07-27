package com.atlas.application.port.out;

import com.atlas.domain.networking.Domain;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates desired Traefik router/service labels for a registered domain.
 * Atlas is control plane; applying labels to a live Traefik instance is left to ops/adapters.
 */
public interface TraefikMetadataPort {

    TraefikRouteMetadata metadataFor(Domain domain, String routerName);

    record TraefikRouteMetadata(
            String routerName,
            String rule,
            String entryPoints,
            boolean tls,
            String certResolver,
            Map<String, String> labels) {

        /** PUBLIC edge: Traefik websecure + TLS cert resolver (Tunnel/DNS path). */
        public static TraefikRouteMetadata of(
                String routerName, String hostname, String certResolver, int backendPort) {
            return of(routerName, hostname, certResolver, backendPort, "websecure", true);
        }

        /**
         * INTERNAL-only: private Traefik entrypoint (LAN). Used when documenting non-public routes;
         * Autopilot PUBLIC path uses {@link #of}; INTERNAL deploy skips public Domain stubs.
         */
        public static TraefikRouteMetadata ofInternal(String routerName, String hostname, int backendPort) {
            return of(routerName, hostname, null, backendPort, "internal", false);
        }

        private static TraefikRouteMetadata of(
                String routerName,
                String hostname,
                String certResolver,
                int backendPort,
                String entryPoints,
                boolean tls) {
            String rule = "Host(`" + hostname + "`)";
            Map<String, String> labels = new LinkedHashMap<>();
            labels.put("traefik.enable", "true");
            labels.put("traefik.http.routers." + routerName + ".rule", rule);
            labels.put("traefik.http.routers." + routerName + ".entrypoints", entryPoints);
            if (tls) {
                labels.put("traefik.http.routers." + routerName + ".tls", "true");
                if (certResolver != null && !certResolver.isBlank()) {
                    labels.put("traefik.http.routers." + routerName + ".tls.certresolver", certResolver);
                }
            }
            labels.put(
                    "traefik.http.services." + routerName + ".loadbalancer.server.port",
                    String.valueOf(backendPort));
            return new TraefikRouteMetadata(routerName, rule, entryPoints, tls, certResolver, labels);
        }
    }
}
