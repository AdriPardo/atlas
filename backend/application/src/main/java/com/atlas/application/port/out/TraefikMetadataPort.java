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

        public static TraefikRouteMetadata of(
                String routerName, String hostname, String certResolver, int backendPort) {
            String rule = "Host(`" + hostname + "`)";
            Map<String, String> labels = new LinkedHashMap<>();
            labels.put("traefik.enable", "true");
            labels.put("traefik.http.routers." + routerName + ".rule", rule);
            labels.put("traefik.http.routers." + routerName + ".entrypoints", "websecure");
            labels.put("traefik.http.routers." + routerName + ".tls", "true");
            labels.put("traefik.http.routers." + routerName + ".tls.certresolver", certResolver);
            labels.put(
                    "traefik.http.services." + routerName + ".loadbalancer.server.port",
                    String.valueOf(backendPort));
            return new TraefikRouteMetadata(routerName, rule, "websecure", true, certResolver, labels);
        }
    }
}
