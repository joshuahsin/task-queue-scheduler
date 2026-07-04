package com.DAO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.entity.Plan;
import com.enums.Enums.PlanTier;

public interface PlanDAO {
    public List<Plan> getPlans();
    public Optional<Plan> getPlanByTier(PlanTier tier);
}
