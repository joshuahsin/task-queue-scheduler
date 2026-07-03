package com.DTO;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.entity.Plan;
import com.enums.Enums.PlanTier;

@Getter
@AllArgsConstructor
public class PlanResponse {
    private UUID id;
    private PlanTier tier;
    private int rateLimit;
    private int maxRetries;
    private int payloadSizeLimit;

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
            plan.getId(), plan.getTier(), plan.getRateLimit(),
            plan.getMaxRetries(), plan.getPayloadSizeLimit()
        );
    }
}
