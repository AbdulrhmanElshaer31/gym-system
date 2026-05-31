package com.gym.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for training sessions (حصص)
 * Sessions are like plans but for multiple individual sessions instead of duration
 */
@Entity
@Table(name = "sessions", indexes = {
        @Index(name = "idx_session_active", columnList = "active"),
        @Index(name = "idx_session_price", columnList = "price")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version = 0L; // Optimistic locking

    @NotBlank(message = "اسم الحصة مطلوب")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "عدد الحصص مطلوب")
    @Min(value = 1, message = "عدد الحصص يجب أن يكون أكبر من صفر")
    @Column(nullable = false)
    private Integer numberOfSessions;

    @NotNull(message = "السعر مطلوب")
    @Min(value = 0, message = "السعر يجب أن يكون صفر أو أكثر")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Min(value = 0, message = "سعر التجديد يجب أن يكون صفر أو أكثر")
    @Column(precision = 10, scale = 2)
    private BigDecimal renewalPrice; // سعر التجديد - إذا كان null يستخدم السعر الأساسي

    @Column(length = 500)
    private String description;

    /**
     * Returns the renewal price, falling back to regular price if not set
     */
    @Transient
    public BigDecimal getEffectiveRenewalPrice() {
        return renewalPrice != null ? renewalPrice : price;
    }

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (version == null) {
            version = 0L;
        }
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (version == null) {
            version = 0L;
        }
        updatedAt = LocalDateTime.now();
    }

    /**
     * Fix for legacy DB rows where @Version column may be NULL.
     */
    @PostLoad
    protected void onLoad() {
        if (version == null) {
            version = 0L;
        }
    }

    @Transient
    public String getSessionsText() {
        if (numberOfSessions == 1) return "حصة واحدة";
        if (numberOfSessions == 5) return "5 حصص";
        if (numberOfSessions == 10) return "10 حصص";
        if (numberOfSessions == 20) return "20 حصة";
        return numberOfSessions + " حصة";
    }
}