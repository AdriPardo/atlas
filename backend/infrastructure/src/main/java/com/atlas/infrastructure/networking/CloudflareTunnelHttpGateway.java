package com.atlas.infrastructure.networking;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Thin HTTP wrapper around Cloudflare API v4 so unit tests can substitute a mock gateway.
 */
@Component
public class CloudflareTunnelHttpGateway {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String get(String url, String bearerToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Cloudflare GET " + response.statusCode() + ": " + truncate(response.body()));
        }
        return response.body();
    }

    public String put(String url, String bearerToken, String jsonBody)
            throws IOException, InterruptedException {
        return send(url, bearerToken, "PUT", jsonBody);
    }

    public String post(String url, String bearerToken, String jsonBody)
            throws IOException, InterruptedException {
        return send(url, bearerToken, "POST", jsonBody);
    }

    public String patch(String url, String bearerToken, String jsonBody)
            throws IOException, InterruptedException {
        return send(url, bearerToken, "PATCH", jsonBody);
    }

    private String send(String url, String bearerToken, String method, String jsonBody)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json");
        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody);
        switch (method) {
            case "PUT" -> builder.PUT(body);
            case "POST" -> builder.POST(body);
            case "PATCH" -> builder.method("PATCH", body);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "Cloudflare " + method + " " + response.statusCode() + ": " + truncate(response.body()));
        }
        return response.body();
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 400 ? body : body.substring(0, 400) + "…";
    }
}
