package com.gym.repository;

import com.gym.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoachRepository extends JpaRepository<Coach, Long> {

    @Query("SELECT c FROM Coach c WHERE c.deleted = false ORDER BY c.name ASC")
    List<Coach> findAllActiveCoaches();

    @Query("SELECT c FROM Coach c WHERE c.deleted = false AND c.active = true ORDER BY c.name ASC")
    List<Coach> findAllWorkingCoaches();

    @Query("SELECT c FROM Coach c WHERE c.deleted = false AND c.name = :name")
    Optional<Coach> findByName(@Param("name") String name);

    @Query("SELECT c FROM Coach c WHERE c.deleted = false AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.specialty) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Coach> searchCoaches(@Param("search") String search);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.coach.id = :coachId AND m.deleted = false")
    Long countMembersByCoach(@Param("coachId") Long coachId);
}