package com.gym.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Entity for tracking all changes to data in the system.
 * Provides complete audit trail for compliance and debugging.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_action", columnList = "action")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String entityType; // "Member", "Transaction", "Product", "Plan"

    @Column(nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(columnDefinition = "TEXT")
    private String oldValue; // JSON representation of old state

    @Column(columnDefinition = "TEXT")
    private String newValue; // JSON representation of new state

    @Column(length = 500)
    private String description; // Human-readable description of the change

    @Column(length = 100)
    private String performedBy; // For future multi-user support

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (performedBy == null) {
            performedBy = "System";
        }
    }

    public enum AuditAction {
        CREATE("إنشاء"),
        UPDATE("تعديل"),
        DELETE("حذف"),
        SOFT_DELETE("حذف مؤقت"),
        RESTORE("استعادة"),
        RENEW("تجديد"),
        SELL("بيع"),
        STOCK_ADD("إضافة مخزون"),
        STOCK_ADJUST("تعديل مخزون"),
        STATUS_CHANGE("تغيير الحالة");

        private final String arabicName;

        AuditAction(String arabicName) {
            this.arabicName = arabicName;
        }

        public String getArabicName() {
            return arabicName;
        }
    }
}
