package com.gym.repository;

import com.gym.entity.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {

    List<SubscriptionHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    @Query("SELECT sh FROM SubscriptionHistory sh WHERE sh.createdAt >= :startDate ORDER BY sh.createdAt DESC")
    List<SubscriptionHistory> findRecentSubscriptions(@Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT COUNT(sh) FROM SubscriptionHistory sh WHERE sh.startDate BETWEEN :start AND :end")
    Long countSubscriptionsBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
