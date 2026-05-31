package com.gym.repository;

import com.gym.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByDeletedFalse();

    List<Member> findByDeletedFalseOrderByCreatedAtDesc();

    @Query("SELECT m FROM Member m WHERE m.deleted = false AND " +
            "(LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "m.phone LIKE CONCAT('%', :search, '%') OR " +
            "m.memberId LIKE CONCAT('%', :search, '%'))")
    List<Member> searchMembers(@Param("search") String search);

    // ✅ NEW: البحث عن المشترك بـ memberId
    @Query("SELECT m FROM Member m WHERE m.deleted = false AND m.memberId = :memberId")
    Optional<Member> findByMemberId(@Param("memberId") String memberId);

    @Query("SELECT m FROM Member m WHERE m.deleted = false AND m.subscriptionEndDate < :date")
    List<Member> findExpiredMembers(@Param("date") LocalDate date);

    @Query("SELECT m FROM Member m WHERE m.deleted = false AND " +
            "m.subscriptionEndDate BETWEEN :startDate AND :endDate")
    List<Member> findMembersNearExpiry(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT m FROM Member m WHERE m.deleted = false AND m.subscriptionEndDate >= :date")
    List<Member> findActiveMembers(@Param("date") LocalDate date);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.deleted = false AND m.subscriptionEndDate >= :date")
    Long countActiveMembers(@Param("date") LocalDate date);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.deleted = false AND m.subscriptionEndDate < :date")
    Long countExpiredMembers(@Param("date") LocalDate date);

    @Query("SELECT m.currentPlan.id, COUNT(m) FROM Member m WHERE m.deleted = false AND m.currentPlan IS NOT NULL GROUP BY m.currentPlan.id")
    List<Object[]> countMembersByPlan();

    @Query("SELECT m FROM Member m WHERE m.deleted = false AND m.currentPlan.id = :planId")
    List<Member> findByPlanId(@Param("planId") Long planId);

    @Query("SELECT m FROM Member m WHERE m.deleted = false AND m.gender = :gender ORDER BY m.createdAt DESC")
    List<Member> findByGender(@Param("gender") Member.Gender gender);

    @Query("SELECT m FROM Member m WHERE m.deleted = false AND m.gender = :gender AND " +
            "(LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "m.phone LIKE CONCAT('%', :search, '%') OR " +
            "m.memberId LIKE CONCAT('%', :search, '%'))")
    List<Member> searchMembersByGender(@Param("search") String search, @Param("gender") Member.Gender gender);

    /**
     * Find all active members with eager loading to prevent LazyInitializationException
     */
    @Query("SELECT DISTINCT m FROM Member m LEFT JOIN FETCH m.coach LEFT JOIN FETCH m.currentPlan " +
            "WHERE m.deleted = FALSE ORDER BY m.createdAt DESC")
    List<Member> findAllActiveWithAssociations();

    boolean existsByPhone(String phone);
}
