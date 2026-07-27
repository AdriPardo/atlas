package com.atlas.infrastructure.proxmox;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import org.springframework.stereotype.Component;

/**
 * Thin HTTP wrapper around Proxmox VE API so unit tests can substitute a mock gateway.
 *
 * <p>Auth header: {@code Authorization: PVEAPIToken=USER@REALM!TOKENID=UUID}.
 */
@Component
public class ProxmoxHttpGateway {

    private final HttpClient defaultClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String get(String url, String apiToken, boolean insecureTls)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "PVEAPIToken=" + apiToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client(insecureTls).send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Proxmox GET " + response.statusCode() + ": " + truncate(response.body()));
        }
        return response.body();
    }

    public String postForm(String url, String apiToken, Map<String, String> form, boolean insecureTls)
            throws IOException, InterruptedException {
        String body = form.entrySet().stream()
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue() == null ? "" : e.getValue()))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "PVEAPIToken=" + apiToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client(insecureTls).send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Proxmox POST " + response.statusCode() + ": " + truncate(response.body()));
        }
        return response.body();
    }

    private HttpClient client(boolean insecureTls) {
        if (!insecureTls) {
            return defaultClient;
        }
        try {
            TrustManager[] trustAll = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, trustAll, new SecureRandom());
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .sslContext(ssl)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build insecure Proxmox HTTP client", e);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 400 ? body : body.substring(0, 400) + "…";
    }
}
