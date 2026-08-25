package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SendProjectMailRequest(
        @NotBlank String to,
        String cc,
        String bcc,
        @NotBlank String subject,
        @NotBlank String textBody,
        String htmlBody,
        String apiToken) {}
