package com.atlas.api.web;

import com.atlas.api.dto.common.PageResponse;
import com.atlas.api.dto.common.PageResponses;
import com.atlas.api.dto.response.AuditEntryResponse;
import com.atlas.application.audit.ExportAuditEntriesUseCase;
import com.atlas.application.audit.ListAuditEntriesUseCase;
import com.atlas.application.shared.PageQuery;
import com.atlas.domain.audit.AuditEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit")
public class AuditController {

    private final ListAuditEntriesUseCase listAuditEntriesUseCase;
    private final ExportAuditEntriesUseCase exportAuditEntriesUseCase;

    @GetMapping
    @Operation(summary = "List audit entries (ADMIN)")
    public ResponseEntity<PageResponse<AuditEntryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var result = listAuditEntriesUseCase.execute(new PageQuery(page, size, sort));
        return ResponseEntity.ok(PageResponses.from(result, this::toResponse));
    }

    @GetMapping("/export")
    @Operation(summary = "Export audit entries as JSON (ADMIN + enterprise audit_export flag)")
    public ResponseEntity<List<AuditEntryResponse>> export() {
        List<AuditEntry> entries = exportAuditEntriesUseCase.execute();
        return ResponseEntity.ok(entries.stream().map(this::toResponse).toList());
    }

    private AuditEntryResponse toResponse(AuditEntry entry) {
        return new AuditEntryResponse(
                entry.getId(),
                entry.getActorUserId(),
                entry.getActorUsername(),
                entry.getAction(),
                entry.getResourceType(),
                entry.getResourceId(),
                entry.getMetadata(),
                entry.getCreatedAt());
    }
}
