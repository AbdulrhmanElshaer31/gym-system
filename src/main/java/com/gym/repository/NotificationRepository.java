package com.gym.repository;

import com.gym.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all unread notifications ordered by priority and creation date
     */
    @Query("SELECT n FROM Notification n WHERE n.isRead = false " +
           "ORDER BY CASE n.priority " +
           "WHEN 'URGENT' THEN 1 " +
           "WHEN 'HIGH' THEN 2 " +
           "WHEN 'MEDIUM' THEN 3 " +
           "WHEN 'LOW' THEN 4 END, n.createdAt DESC")
    List<Notification> findUnreadNotifications();

    /**
     * Find notifications by type
     */
    List<Notification> findByTypeOrderByCreatedAtDesc(Notification.NotificationType type);

    /**
     * Find unread notifications by type
     */
    List<Notification> findByTypeAndIsReadFalseOrderByCreatedAtDesc(Notification.NotificationType type);

    /**
     * Count unread notifications
     */
    long countByIsReadFalse();

    /**
     * Count unread notifications by priority
     */
    long countByIsReadFalseAndPriority(Notification.NotificationPriority priority);

    /**
     * Find notifications for a specific entity
     */
    List<Notification> findByRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId);

    /**
     * Mark all as read
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.isRead = false")
    int markAllAsRead(@Param("now") LocalDateTime now);

    /**
     * Delete expired notifications
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    int deleteExpiredNotifications(@Param("now") LocalDateTime now);

    /**
     * Delete old read notifications
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.readAt < :before")
    int deleteOldReadNotifications(@Param("before") LocalDateTime before);

    /**
     * Find recent notifications (for dashboard)
     */
    @Query("SELECT n FROM Notification n ORDER BY n.createdAt DESC LIMIT :limit")
    List<Notification> findRecentNotifications(@Param("limit") int limit);

    /**
     * Check if notification already exists for entity (to avoid duplicates)
     */
    boolean existsByTypeAndRelatedEntityTypeAndRelatedEntityIdAndIsReadFalse(
            Notification.NotificationType type, String entityType, Long entityId);
}
