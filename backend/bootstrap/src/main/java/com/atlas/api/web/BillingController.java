package com.atlas.api.web;

import com.atlas.api.dto.common.PageResponse;
import com.atlas.api.dto.common.PageResponses;
import com.atlas.api.dto.response.PlanEntitlementsResponse;
import com.atlas.api.dto.response.UsageRecordResponse;
import com.atlas.application.billing.GetPlanEntitlementsUseCase;
import com.atlas.application.billing.ListUsageRecordsUseCase;
import com.atlas.application.shared.PageQuery;
import com.atlas.domain.billing.PlanEntitlement;
import com.atlas.domain.billing.UsageRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Billing")
public class BillingController {

    private final ListUsageRecordsUseCase listUsageRecordsUseCase;
    private final GetPlanEntitlementsUseCase getPlanEntitlementsUseCase;

    @GetMapping("/usage")
    @Operation(summary = "List usage meter records (ADMIN)")
    public ResponseEntity<PageResponse<UsageRecordResponse>> usage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var result = listUsageRecordsUseCase.execute(new PageQuery(page, size, sort));
        return ResponseEntity.ok(PageResponses.from(result, this::toUsageResponse));
    }

    @GetMapping("/entitlements")
    @Operation(summary = "Current plan entitlements and live gauges (ADMIN)")
    public ResponseEntity<PlanEntitlementsResponse> entitlements() {
        GetPlanEntitlementsUseCase.Result result = getPlanEntitlementsUseCase.execute();
        return ResponseEntity.ok(new PlanEntitlementsResponse(
                result.planCode(),
                result.entitlements().stream().map(this::toEntitlementResponse).toList(),
                result.gauges().stream()
                        .map(g -> new PlanEntitlementsResponse.LiveGaugeResponse(g.meter(), g.quantity()))
                        .toList()));
    }

    private UsageRecordResponse toUsageResponse(UsageRecord record) {
        return new UsageRecordResponse(
                record.getId(),
                record.getMeter(),
                record.getQuantity(),
                record.getPeriodStart(),
                record.getPeriodEnd(),
                record.getDimensions(),
                record.getCreatedAt());
    }

    private PlanEntitlementsResponse.PlanEntitlementResponse toEntitlementResponse(PlanEntitlement e) {
        return new PlanEntitlementsResponse.PlanEntitlementResponse(
                e.getPlanCode(),
                e.getMeter(),
                e.getLimitQuantity(),
                e.getUnit(),
                e.getPriceCents(),
                e.isSoft(),
                e.isUnlimited());
    }
}
