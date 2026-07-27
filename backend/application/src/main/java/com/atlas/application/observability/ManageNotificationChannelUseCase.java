package com.atlas.application.observability;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.NotificationChannelRepositoryPort;
import com.atlas.domain.observability.NotificationChannel;
import com.atlas.domain.observability.NotificationChannelType;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.user.Role;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageNotificationChannelUseCase {

    private final NotificationChannelRepositoryPort channelRepository;
    private final ProjectAuthorizationService authorizationService;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional(readOnly = true)
    public List<NotificationChannel> list() {
        requireOperatorOrAdmin();
        return channelRepository.findAll();
    }

    @Transactional(readOnly = true)
    public NotificationChannel get(UUID channelId) {
        requireOperatorOrAdmin();
        return requireChannel(channelId);
    }

    @Transactional
    public NotificationChannel create(String name, NotificationChannelType type, String target) {
        requireOperatorOrAdmin();
        if (channelRepository.existsByNameIgnoreCase(name.trim())) {
            throw new ConflictException("Notification channel name already exists");
        }
        NotificationChannel saved = channelRepository.save(NotificationChannel.create(name, type, target));
        recordAuditUseCase.execute(
                "NOTIFICATION_CHANNEL_CREATE",
                "notification_channel",
                saved.getId(),
                "{\"name\":\"" + saved.getName() + "\",\"type\":\"" + saved.getType() + "\"}");
        return saved;
    }

    @Transactional
    public NotificationChannel update(
            UUID channelId, String name, NotificationChannelType type, String target, Boolean enabled) {
        requireOperatorOrAdmin();
        NotificationChannel channel = requireChannel(channelId);
        if (channelRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), channelId)) {
            throw new ConflictException("Notification channel name already exists");
        }
        channel.update(name, type, target, enabled);
        NotificationChannel saved = channelRepository.save(channel);
        recordAuditUseCase.execute(
                "NOTIFICATION_CHANNEL_UPDATE",
                "notification_channel",
                saved.getId(),
                "{\"name\":\"" + saved.getName() + "\"}");
        return saved;
    }

    @Transactional
    public void delete(UUID channelId) {
        requireOperatorOrAdmin();
        NotificationChannel channel = requireChannel(channelId);
        channelRepository.deleteById(channelId);
        recordAuditUseCase.execute(
                "NOTIFICATION_CHANNEL_DELETE",
                "notification_channel",
                channelId,
                "{\"name\":\"" + channel.getName() + "\"}");
    }

    private void requireOperatorOrAdmin() {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin() && actor.role() != Role.OPERATOR) {
            throw new ForbiddenException("ADMIN or OPERATOR required");
        }
    }

    private NotificationChannel requireChannel(UUID channelId) {
        return channelRepository
                .findById(channelId)
                .orElseThrow(() -> new NotFoundException("Notification channel not found: " + channelId));
    }
}
