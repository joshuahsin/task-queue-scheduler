package com.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.DAO.PlanDAO;
import com.entity.Plan;
import com.enums.Enums.PlanTier;
import com.repo.PlanRepo;

@Service
public class PlanService implements PlanDAO {
    private final PlanRepo planRepo;

    public PlanService(PlanRepo planRepo) {
        this.planRepo = planRepo;
    }

    @Override
    public List<Plan> getPlans() {
        return planRepo.findAll();
    }

    @Override
    public Optional<Plan> getPlanByTier(PlanTier tier) {
        return planRepo.findByTier(tier);
    }
}
