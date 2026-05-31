package com.gym.service;

import com.gym.entity.Member;
import com.gym.entity.Notification;
import com.gym.entity.Notification.NotificationPriority;
import com.gym.entity.Notification.NotificationType;
import com.gym.entity.Product;
import com.gym.repository.MemberRepository;
import com.gym.repository.NotificationRepository;
import com.gym.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing system notifications.
 * Automatically generates alerts for important events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    /**
     * Get all unread notifications
     */
    public List<Notification> getUnreadNotifications() {
        return notificationRepository.findUnreadNotifications();
    }

    /**
     * Get recent notifications (for dashboard)
     */
    public List<Notification> getRecentNotifications(int limit) {
        return notificationRepository.findRecentNotifications(limit);
    }

    /**
     * Count unread notifications
     */
    public long countUnread() {
        return notificationRepository.countByIsReadFalse();
    }

    /**
     * Count urgent unread notifications
     */
    public long countUrgentUnread() {
        return notificationRepository.countByIsReadFalseAndPriority(NotificationPriority.URGENT);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public int markAllAsRead() {
        return notificationRepository.markAllAsRead(LocalDateTime.now());
    }

    /**
     * Create a notification
     */
    @Transactional
    public Notification createNotification(NotificationType type, NotificationPriority priority,
                                           String title, String message,
                                           String entityType, Long entityId) {
        // Check if similar notification already exists
        if (entityType != null && entityId != null) {
            boolean exists = notificationRepository.existsByTypeAndRelatedEntityTypeAndRelatedEntityIdAndIsReadFalse(
                    type, entityType, entityId);
            if (exists) {
                return null; // Avoid duplicate notifications
            }
        }

        Notification notification = Notification.builder()
                .type(type)
                .priority(priority)
                .title(title)
                .message(message)
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Create a simple notification
     */
    @Transactional
    public Notification createNotification(NotificationType type, String title, String message) {
        return createNotification(type, NotificationPriority.MEDIUM, title, message, null, null);
    }

    /**
     * Scheduled task: Check for expiring subscriptions (runs daily at 9 AM)
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void checkExpiringSubscriptions() {
        log.info("Checking for expiring subscriptions...");
        
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);
        
        // Find members near expiry (within 7 days)
        List<Member> nearExpiry = memberRepository.findMembersNearExpiry(today, sevenDaysFromNow);
        
        for (Member member : nearExpiry) {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, member.getSubscriptionEndDate());
            
            NotificationPriority priority = daysLeft <= 2 ? NotificationPriority.HIGH : NotificationPriority.MEDIUM;
            
            createNotification(
                    NotificationType.SUBSCRIPTION_EXPIRING,
                    priority,
                    "اشتراك على وشك الانتهاء",
                    String.format("اشتراك %s سينتهي خلال %d يوم", member.getName(), daysLeft),
                    "Member",
                    member.getId()
            );
        }
        
        // Find expired members
        List<Member> expired = memberRepository.findExpiredMembers(today);
        
        for (Member member : expired) {
            createNotification(
                    NotificationType.SUBSCRIPTION_EXPIRED,
                    NotificationPriority.HIGH,
                    "اشتراك منتهي",
                    String.format("اشتراك %s انتهى في %s", member.getName(), member.getSubscriptionEndDate()),
                    "Member",
                    member.getId()
            );
        }
        
        log.info("Found {} near expiry and {} expired subscriptions", nearExpiry.size(), expired.size());
    }

    /**
     * Scheduled task: Check for low stock products (runs daily at 8 AM)
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkLowStockProducts() {
        log.info("Checking for low stock products...");
        
        List<Product> lowStock = productRepository.findLowStockProducts();
        
        for (Product product : lowStock) {
            NotificationPriority priority = product.getQuantity() == 0 
                    ? NotificationPriority.URGENT 
                    : NotificationPriority.HIGH;
            
            NotificationType type = product.getQuantity() == 0 
                    ? NotificationType.OUT_OF_STOCK 
                    : NotificationType.LOW_STOCK;
            
            createNotification(
                    type,
                    priority,
                    product.getQuantity() == 0 ? "نفاد المخزون" : "مخزون منخفض",
                    String.format("%s - الكمية المتبقية: %d", product.getName(), product.getQuantity()),
                    "Product",
                    product.getId()
            );
        }
        
        log.info("Found {} low stock products", lowStock.size());
    }

    /**
     * Notify about new member
     */
    @Transactional
    public void notifyNewMember(Member member) {
        createNotification(
                NotificationType.NEW_MEMBER,
                NotificationPriority.LOW,
                "مشترك جديد",
                String.format("تم تسجيل مشترك جديد: %s", member.getName()),
                "Member",
                member.getId()
        );
    }

    /**
     * Notify about subscription renewal
     */
    @Transactional
    public void notifyRenewal(Member member) {
        createNotification(
                NotificationType.RENEWAL,
                NotificationPriority.LOW,
                "تجديد اشتراك",
                String.format("تم تجديد اشتراك %s حتى %s", member.getName(), member.getSubscriptionEndDate()),
                "Member",
                member.getId()
        );
    }

    /**
     * Notify about backup creation
     */
    @Transactional
    public void notifyBackupCreated(String backupPath) {
        createNotification(
                NotificationType.BACKUP_CREATED,
                NotificationPriority.LOW,
                "نسخة احتياطية",
                "تم إنشاء نسخة احتياطية بنجاح: " + backupPath,
                null,
                null
        );
    }

    /**
     * Scheduled cleanup: Remove old read notifications (runs weekly on Sunday at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Cleaning up old notifications...");
        
        // Delete expired notifications
        int expiredDeleted = notificationRepository.deleteExpiredNotifications(LocalDateTime.now());
        
        // Delete read notifications older than 30 days
        int oldReadDeleted = notificationRepository.deleteOldReadNotifications(
                LocalDateTime.now().minusDays(30));
        
        log.info("Deleted {} expired and {} old read notifications", expiredDeleted, oldReadDeleted);
    }

    /**
     * Get notifications for a specific entity
     */
    public List<Notification> getNotificationsForEntity(String entityType, Long entityId) {
        return notificationRepository.findByRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc(
                entityType, entityId);
    }

    /**
     * Delete a notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
