package com.gym.repository;

import com.gym.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for a specific entity
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);

    /**
     * Find recent activity with pagination
     */
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Find logs by entity type
     */
    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);

    /**
     * Find logs by action type
     */
    List<AuditLog> findByActionOrderByTimestampDesc(AuditLog.AuditAction action);

    /**
     * Find logs within a date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    List<AuditLog> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count actions by type for statistics
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end GROUP BY a.action")
    List<Object[]> countActionsByType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get the most recent logs (limited)
     */
    @Query("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC LIMIT :limit")
    List<AuditLog> findRecentLogs(@Param("limit") int limit);

    /**
     * Delete old logs for cleanup
     */
    void deleteByTimestampBefore(LocalDateTime before);
}
