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

@Entity
@Table(name = "plans", indexes = {
        @Index(name = "idx_plan_active", columnList = "active"),
        @Index(name = "idx_plan_price", columnList = "price")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version = 0L;

    @NotBlank(message = "اسم الخطة مطلوب")
    @Column(nullable = false)
    private String name;

    // ✅ الفترة الزمنية (بالأيام)
    @NotNull(message = "المدة مطلوبة")
    @Min(value = 1, message = "المدة يجب أن تكون أكبر من صفر")
    @Column(nullable = false)
    private Integer durationDays;

    // ✅ عدد الحصص (اختياري)
    @Min(value = 0, message = "عدد الحصص يجب أن يكون صفر أو أكثر")
    @Column
    private Integer numberOfSessions;

    @NotNull(message = "السعر مطلوب")
    @Min(value = 0, message = "السعر يجب أن يكون صفر أو أكثر")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Min(value = 0, message = "سعر التجديد يجب أن يكون صفر أو أكثر")
    @Column(precision = 10, scale = 2)
    private BigDecimal renewalPrice;

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

    @PostLoad
    protected void onLoad() {
        if (version == null) {
            version = 0L;
        }
    }

    // ✅ نص العرض للفترة الزمنية
    @Transient
    public String getDurationText() {
        if (durationDays == 30) return "شهر واحد";
        if (durationDays == 60) return "شهرين";
        if (durationDays == 90) return "3 أشهر";
        if (durationDays == 180) return "6 أشهر";
        if (durationDays == 365) return "سنة";
        return durationDays + " يوم";
    }

    // ✅ نص العرض للحصص
    @Transient
    public String getSessionsText() {
        if (numberOfSessions == null || numberOfSessions == 0) {
            return "بدون تحديد حصص";
        }
        return numberOfSessions + " حصة";
    }

    // ✅ نص مدمج يجمع بين الفترة والحصص
    @Transient
    public String getCombinedText() {
        String duration = getDurationText();
        String sessions = getSessionsText();

        if (numberOfSessions == null || numberOfSessions == 0) {
            return duration; // فقط الفترة الزمنية
        }

        return duration + " (" + numberOfSessions + " حصة)"; // الفترة والحصص
    }
}