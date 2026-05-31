package com.gym.repository;

import com.gym.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByActiveTrue();

    List<Plan> findByActiveTrueOrderByPriceAsc();

    List<Plan> findAllByOrderByCreatedAtDesc();

    @Query("SELECT p FROM Plan p ORDER BY p.active DESC, p.price ASC")
    List<Plan> findAllOrderByActiveAndPrice();

    boolean existsByName(String name);
}