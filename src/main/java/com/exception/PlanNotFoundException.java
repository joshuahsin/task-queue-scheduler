package com.exception;

import com.enums.Enums.PlanTier;

public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException(PlanTier tier) {
        super("Plan not found for tier: " + tier);
    }
}
