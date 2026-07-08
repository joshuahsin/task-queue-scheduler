package com.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.entity.Plan;
import com.enums.Enums.PlanTier;
import com.repo.PlanRepo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepo planRepo;

    private PlanService planService;

    @BeforeEach
    void setUp() {
        planService = new PlanService(planRepo);
    }

    @Test
    void getPlans_delegatesToRepo() {
        Plan plan = new Plan();
        when(planRepo.findAll()).thenReturn(List.of(plan));

        assertThat(planService.getPlans()).containsExactly(plan);
    }

    @Test
    void getPlanByTier_delegatesToRepo() {
        Plan plan = new Plan();
        when(planRepo.findByTier(PlanTier.PREMIUM)).thenReturn(Optional.of(plan));

        assertThat(planService.getPlanByTier(PlanTier.PREMIUM)).contains(plan);
    }

    @Test
    void getPlanByTier_returnsEmptyWhenNoSuchTier() {
        when(planRepo.findByTier(PlanTier.FREE)).thenReturn(Optional.empty());

        assertThat(planService.getPlanByTier(PlanTier.FREE)).isEmpty();
    }
}
