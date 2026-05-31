package com.gym.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Entity for system notifications.
 * Tracks subscription expirations, low stock alerts, and other important events.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_read", columnList = "isRead"),
    @Index(name = "idx_notification_type", columnList = "type"),
    @Index(name = "idx_notification_created", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(length = 50)
    private String relatedEntityType; // "Member", "Product", etc.

    private Long relatedEntityId;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column
    private LocalDateTime readAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt; // Optional expiration for temporary notifications

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (priority == null) {
            priority = NotificationPriority.MEDIUM;
        }
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public enum NotificationType {
        SUBSCRIPTION_EXPIRING("اشتراك على وشك الانتهاء", "⚠️"),
        SUBSCRIPTION_EXPIRED("اشتراك منتهي", "❌"),
        LOW_STOCK("مخزون منخفض", "📦"),
        OUT_OF_STOCK("نفاد المخزون", "🚫"),
        DAILY_SUMMARY("ملخص يومي", "📊"),
        WEEKLY_SUMMARY("ملخص أسبوعي", "📈"),
        PAYMENT_RECEIVED("دفعة مستلمة", "💰"),
        NEW_MEMBER("مشترك جديد", "👤"),
        RENEWAL("تجديد اشتراك", "🔄"),
        BACKUP_CREATED("نسخة احتياطية", "💾"),
        SYSTEM_ALERT("تنبيه النظام", "🔔");

        private final String arabicName;
        private final String icon;

        NotificationType(String arabicName, String icon) {
            this.arabicName = arabicName;
            this.icon = icon;
        }

        public String getArabicName() {
            return arabicName;
        }

        public String getIcon() {
            return icon;
        }
    }

    public enum NotificationPriority {
        LOW("منخفضة"),
        MEDIUM("متوسطة"),
        HIGH("عالية"),
        URGENT("عاجلة");

        private final String arabicName;

        NotificationPriority(String arabicName) {
            this.arabicName = arabicName;
        }

        public String getArabicName() {
            return arabicName;
        }
    }
}
