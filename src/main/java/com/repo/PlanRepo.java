package com.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.entity.Plan;
import com.enums.Enums.PlanTier;

public interface PlanRepo extends JpaRepository<Plan, UUID> {
    Optional<Plan> findByTier(PlanTier tier);
}
