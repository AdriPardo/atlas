package com.atlas.infrastructure.adapter.git;

import com.atlas.application.pipeline.GithubRepositoryUrlParser;
import com.atlas.application.port.out.GitProviderWebhookPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GitHubWebhookAdapter implements GitProviderWebhookPort {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookAdapter.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RegisterResult registerPushWebhook(
            String repositoryUrl, String webhookUrl, String secret, String accessToken) {
        Optional<GithubRepositoryUrlParser.OwnerRepo> parsed = GithubRepositoryUrlParser.parse(repositoryUrl);
        if (parsed.isEmpty()) {
            return RegisterResult.skipped("Could not parse GitHub owner/repo from repository URL.");
        }
        String owner = parsed.get().owner();
        String repo = parsed.get().repo();
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/hooks";

        try {
            Optional<String> existing = findExistingHookId(apiUrl, accessToken, webhookUrl);
            if (existing.isPresent()) {
                return RegisterResult.ok(
                        "GitHub webhook already points at this Atlas URL.", existing.get());
            }

            String body = """
                    {
                      "name": "web",
                      "active": true,
                      "events": ["push"],
                      "config": {
                        "url": %s,
                        "content_type": "json",
                        "secret": %s,
                        "insecure_ssl": "0"
                      }
                    }
                    """
                    .formatted(objectMapper.writeValueAsString(webhookUrl), objectMapper.writeValueAsString(secret));

            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                String hookId = readHookId(response.body());
                return RegisterResult.ok("GitHub push webhook created.", hookId);
            }
            if (response.statusCode() == 422) {
                // Likely duplicate — try to find it again
                Optional<String> again = findExistingHookId(apiUrl, accessToken, webhookUrl);
                if (again.isPresent()) {
                    return RegisterResult.ok("GitHub webhook already exists for this URL.", again.get());
                }
            }
            log.warn("GitHub webhook create failed: {} {}", response.statusCode(), truncate(response.body()));
            return RegisterResult.failed(
                    "GitHub API returned " + response.statusCode() + "; register the webhook manually.");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("GitHub webhook register error: {}", e.toString());
            return RegisterResult.failed("Could not reach GitHub API; register the webhook manually.");
        }
    }

    private Optional<String> findExistingHookId(String apiUrl, String accessToken, String webhookUrl)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + "?per_page=100"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return Optional.empty();
        }
        JsonNode arr = objectMapper.readTree(response.body());
        if (!arr.isArray()) {
            return Optional.empty();
        }
        for (JsonNode hook : arr) {
            JsonNode config = hook.get("config");
            if (config == null) {
                continue;
            }
            JsonNode url = config.get("url");
            if (url != null && webhookUrl.equals(url.asText())) {
                JsonNode id = hook.get("id");
                return id == null ? Optional.empty() : Optional.of(id.asText());
            }
        }
        return Optional.empty();
    }

    private String readHookId(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode id = node.get("id");
            return id == null ? null : id.asText();
        } catch (IOException e) {
            return null;
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 300 ? body : body.substring(0, 300) + "…";
    }
}
