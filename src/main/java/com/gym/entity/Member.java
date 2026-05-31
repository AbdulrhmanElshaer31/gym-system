package com.gym.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "members", indexes = {
        @Index(name = "idx_member_phone", columnList = "phone"),
        @Index(name = "idx_member_subscription_end", columnList = "subscriptionEndDate"),
        @Index(name = "idx_member_plan", columnList = "plan_id"),
        @Index(name = "idx_member_deleted", columnList = "deleted"),
        @Index(name = "idx_member_member_id", columnList = "memberId"),
        @Index(name = "idx_member_gender", columnList = "gender"),
        @Index(name = "idx_member_coach", columnList = "coach_id")
})
@DynamicUpdate
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(unique = true)
    private String memberId;

    @NotBlank(message = "الاسم مطلوب")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "رقم الهاتف مطلوب")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "رقم الهاتف غير صحيح")
    @Column(nullable = false)
    private String phone;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Gender gender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coach_id")
    private Coach coach;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id")
    private Plan currentPlan;

    @Column(nullable = false)
    private LocalDate subscriptionStartDate;

    @Column(nullable = false)
    private LocalDate subscriptionEndDate;

    @Column(nullable = false)
    private Integer renewalCount = 0;

    // ✅ عدد الحصص المتبقية
    @Column(nullable = false)
    private Integer remainingSessions = 0;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SubscriptionHistory> subscriptionHistory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ UPDATED: getStatus - يأخذ بالحسبان الحصص والأيام معاً
    @Transient
    public SubscriptionStatus getStatus() {
        LocalDate today = LocalDate.now();

        // Check if subscription date expired
        if (subscriptionEndDate.isBefore(today)) {
            return SubscriptionStatus.EXPIRED;
        }

        // Check if sessions expired (if plan has sessions limit)
        if (currentPlan != null &&
                currentPlan.getNumberOfSessions() != null &&
                currentPlan.getNumberOfSessions() > 0 &&
                remainingSessions <= 0) {
            return SubscriptionStatus.EXPIRED;
        }

        // Check if near expiry by date
        if (subscriptionEndDate.minusDays(7).isBefore(today)) {
            return SubscriptionStatus.NEAR_EXPIRY;
        }

        // Check if near expiry by sessions (last 5 sessions)
        if (currentPlan != null &&
                currentPlan.getNumberOfSessions() != null &&
                currentPlan.getNumberOfSessions() > 0 &&
                remainingSessions > 0 &&
                remainingSessions <= 5) {
            return SubscriptionStatus.NEAR_EXPIRY;
        }

        return SubscriptionStatus.ACTIVE;
    }

    // ✅ check if sessions are available
    @Transient
    public boolean hasAvailableSessions() {
        if (currentPlan == null || currentPlan.getNumberOfSessions() == null) {
            return true; // no sessions limit
        }
        return remainingSessions > 0;
    }

    // ✅ get sessions status similar to subscription status
    @Transient
    public SessionsStatus getSessionsStatus() {
        if (currentPlan == null || currentPlan.getNumberOfSessions() == null || currentPlan.getNumberOfSessions() == 0) {
            return SessionsStatus.UNLIMITED; // no limit
        }

        if (remainingSessions <= 0) {
            return SessionsStatus.EXPIRED;
        } else if (remainingSessions <= 5) {
            return SessionsStatus.NEAR_EXPIRY;
        }
        return SessionsStatus.ACTIVE;
    }

    // ✅ Getter و Setter للحصص المتبقية
    public Integer getRemainingSession() {
        return remainingSessions;
    }

    public void setRemainingSession(Integer remainingSessions) {
        this.remainingSessions = remainingSessions;
    }

    public enum Gender {
        MALE("ذكر"),
        FEMALE("أنثى");

        private final String arabicName;

        Gender(String arabicName) {
            this.arabicName = arabicName;
        }

        public String getArabicName() {
            return arabicName;
        }
    }

    public enum SubscriptionStatus {
        ACTIVE("نشط", "#22c55e"),
        NEAR_EXPIRY("قارب على الانتهاء", "#f59e0b"),
        EXPIRED("منتهي", "#ef4444");

        private final String arabicName;
        private final String color;

        SubscriptionStatus(String arabicName, String color) {
            this.arabicName = arabicName;
            this.color = color;
        }

        public String getArabicName() {
            return arabicName;
        }

        public String getColor() {
            return color;
        }
    }

    // ✅ Sessions Status Enum
    public enum SessionsStatus {
        ACTIVE("متاح", "#22c55e"),
        NEAR_EXPIRY("قريب الانتهاء", "#f59e0b"),
        EXPIRED("منتهي", "#ef4444"),
        UNLIMITED("بدون حد", "#3b82f6");

        private final String arabicName;
        private final String color;

        SessionsStatus(String arabicName, String color) {
            this.arabicName = arabicName;
            this.color = color;
        }

        public String getArabicName() {
            return arabicName;
        }

        public String getColor() {
            return color;
        }
    }
}