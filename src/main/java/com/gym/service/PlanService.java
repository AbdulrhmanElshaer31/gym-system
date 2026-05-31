package com.gym.service;

import com.gym.entity.Plan;
import com.gym.repository.MemberRepository;
import com.gym.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final MemberRepository memberRepository;

    public List<Plan> getAllPlans() {
        return planRepository.findAllOrderByActiveAndPrice();
    }

    public List<Plan> getActivePlans() {
        return planRepository.findByActiveTrueOrderByPriceAsc();
    }

    public Optional<Plan> getPlanById(Long id) {
        return planRepository.findById(id);
    }

    @Transactional
    public Plan createPlan(Plan plan) {
        plan.setActive(true);
        plan.setVersion(0L); // Initialize version explicitly
        return planRepository.save(plan);
    }

    @Transactional
    public Plan updatePlan(Long id, Plan updatedPlan) {
        return planRepository.findById(id)
                .map(plan -> {
                    plan.setName(updatedPlan.getName());
                    plan.setDurationDays(updatedPlan.getDurationDays());
                    plan.setNumberOfSessions(updatedPlan.getNumberOfSessions());  // ✅ جديد
                    plan.setPrice(updatedPlan.getPrice());
                    plan.setRenewalPrice(updatedPlan.getRenewalPrice());
                    plan.setDescription(updatedPlan.getDescription());
                    plan.setActive(updatedPlan.getActive());
                    return planRepository.save(plan);
                })
                .orElseThrow(() -> new RuntimeException("الخطة غير موجودة"));
    }

    @Transactional
    public void togglePlanStatus(Long id) {
        planRepository.findById(id)
                .ifPresent(plan -> {
                    plan.setActive(!plan.getActive());
                    planRepository.save(plan);
                });
    }

    public boolean canDeletePlan(Long planId) {
        return memberRepository.findByPlanId(planId).isEmpty();
    }

    @Transactional
    public void deletePlan(Long id) {
        if (!canDeletePlan(id)) {
            throw new RuntimeException("لا يمكن حذف خطة مستخدمة من قبل مشتركين");
        }
        planRepository.deleteById(id);
    }

    public Map<Long, Long> getMembersCountByPlan() {
        Map<Long, Long> result = new HashMap<>();
        memberRepository.countMembersByPlan().forEach(row -> {
            result.put((Long) row[0], (Long) row[1]);
        });
        return result;
    }

    public Plan getMostPopularPlan() {
        Map<Long, Long> counts = getMembersCountByPlan();
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> planRepository.findById(entry.getKey()).orElse(null))
                .orElse(null);
    }
}