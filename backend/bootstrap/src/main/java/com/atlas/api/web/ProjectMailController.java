package com.atlas.api.web;

import com.atlas.api.dto.request.SendProjectMailRequest;
import com.atlas.api.dto.response.ProjectMailProvisionResponse;
import com.atlas.api.dto.response.ProjectMailResponse;
import com.atlas.api.dto.response.SendProjectMailResponse;
import com.atlas.application.mail.ProvisionProjectMailUseCase;
import com.atlas.application.mail.SendProjectMailUseCase;
import com.atlas.application.port.out.ProjectSmtpProvisionerPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/mail")
@RequiredArgsConstructor
@Tag(name = "Project mail")
public class ProjectMailController {

    private final ProvisionProjectMailUseCase provisionProjectMailUseCase;
    private final SendProjectMailUseCase sendProjectMailUseCase;
    private final ProjectSmtpProvisionerPort smtpProvisioner;

    @GetMapping
    @Operation(summary = "Project mail status (never returns credentials)")
    public ResponseEntity<ProjectMailResponse> status(@org.springframework.web.bind.annotation.PathVariable UUID projectId) {
        var status = provisionProjectMailUseCase.status(projectId);
        int dailyLimit = smtpProvisioner.isConfigured() ? smtpProvisioner.dailySendLimitPerProject() : 0;
        int remaining = status.provisioned() ? sendProjectMailUseCase.remainingToday(projectId) : 0;
        return ResponseEntity.ok(new ProjectMailResponse(
                status.provisionerConfigured(),
                status.provisioned(),
                status.from(),
                status.host(),
                status.port(),
                status.tls(),
                status.message(),
                remaining,
                dailyLimit));
    }

    @PostMapping("/provision")
    @Operation(summary = "Provision SMTP secrets (smtp.* + mail.api_token) for deploy envFrom")
    public ResponseEntity<ProjectMailProvisionResponse> provision(
            @org.springframework.web.bind.annotation.PathVariable UUID projectId) {
        var outcome = provisionProjectMailUseCase.provision(projectId);
        return ResponseEntity.ok(new ProjectMailProvisionResponse(
                outcome.from(), outcome.host(), outcome.port(), outcome.tls(), outcome.rotated()));
    }

    @PostMapping("/send")
    @Operation(summary = "Send transactional email via platform SMTP relay")
    public ResponseEntity<SendProjectMailResponse> send(
            @org.springframework.web.bind.annotation.PathVariable UUID projectId,
            @Valid @RequestBody SendProjectMailRequest body,
            @RequestHeader(value = "X-Atlas-Mail-Token", required = false) String mailTokenHeader) {
        Optional<String> apiToken = Optional.ofNullable(body.apiToken())
                .filter(t -> !t.isBlank())
                .or(() -> Optional.ofNullable(mailTokenHeader).filter(t -> !t.isBlank()));
        var result = sendProjectMailUseCase.execute(
                projectId,
                new SendProjectMailUseCase.SendMailCommand(
                        body.to(),
                        Optional.ofNullable(body.cc()).filter(s -> !s.isBlank()),
                        Optional.ofNullable(body.bcc()).filter(s -> !s.isBlank()),
                        body.subject(),
                        body.textBody(),
                        Optional.ofNullable(body.htmlBody()).filter(s -> !s.isBlank()),
                        apiToken));
        return ResponseEntity.ok(new SendProjectMailResponse(result.sent(), result.detail(), result.remainingToday()));
    }
}
