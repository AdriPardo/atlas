package com.atlas.api.web;

import com.atlas.api.dto.request.CreateNotificationChannelRequest;
import com.atlas.api.dto.request.UpdateNotificationChannelRequest;
import com.atlas.api.dto.response.NotificationChannelResponse;
import com.atlas.application.observability.ManageNotificationChannelUseCase;
import com.atlas.domain.observability.NotificationChannel;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-channels")
@RequiredArgsConstructor
public class NotificationChannelController {

    private final ManageNotificationChannelUseCase manageNotificationChannelUseCase;

    @GetMapping
    public ResponseEntity<List<NotificationChannelResponse>> list() {
        return ResponseEntity.ok(manageNotificationChannelUseCase.list().stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<NotificationChannelResponse> get(@PathVariable UUID channelId) {
        return ResponseEntity.ok(toResponse(manageNotificationChannelUseCase.get(channelId)));
    }

    @PostMapping
    public ResponseEntity<NotificationChannelResponse> create(
            @Valid @RequestBody CreateNotificationChannelRequest request) {
        NotificationChannel channel =
                manageNotificationChannelUseCase.create(request.name(), request.type(), request.target());
        return ResponseEntity.created(URI.create("/api/v1/notification-channels/" + channel.getId()))
                .body(toResponse(channel));
    }

    @PutMapping("/{channelId}")
    public ResponseEntity<NotificationChannelResponse> update(
            @PathVariable UUID channelId, @Valid @RequestBody UpdateNotificationChannelRequest request) {
        return ResponseEntity.ok(toResponse(manageNotificationChannelUseCase.update(
                channelId, request.name(), request.type(), request.target(), request.enabled())));
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<Void> delete(@PathVariable UUID channelId) {
        manageNotificationChannelUseCase.delete(channelId);
        return ResponseEntity.noContent().build();
    }

    private NotificationChannelResponse toResponse(NotificationChannel channel) {
        return new NotificationChannelResponse(
                channel.getId(),
                channel.getName(),
                channel.getType(),
                channel.getTarget(),
                channel.isEnabled(),
                channel.getCreatedAt(),
                channel.getUpdatedAt());
    }
}
