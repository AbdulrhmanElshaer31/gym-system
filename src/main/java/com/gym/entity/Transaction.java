package com.gym.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_date", columnList = "transactionDate"),
        @Index(name = "idx_transaction_type", columnList = "type"),
        @Index(name = "idx_transaction_category", columnList = "category"),
        @Index(name = "idx_transaction_member", columnList = "member_id")
})
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;  // إزالة التهيئة اليدوية

    @NotNull(message = "نوع المعاملة مطلوب")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @NotNull(message = "التصنيف مطلوب")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionCategory category;

    @NotNull(message = "المبلغ مطلوب")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "الوصف مطلوب")
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
        if (version == null) {
            version = 0L;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public enum TransactionType {
        INCOME("إيراد"),
        EXPENSE("مصروف");

        private final String arabicName;

        TransactionType(String arabicName) {
            this.arabicName = arabicName;
        }

        public String getArabicName() {
            return arabicName;
        }
    }

    public enum TransactionCategory {
        SUBSCRIPTION("اشتراك"),
        RENEWAL("تجديد"),
        PRODUCT_SALE("مبيعات منتجات"),
        OTHER_INCOME("دخل آخر"),
        SALARY("مرتبات"),
        RENT("إيجار"),
        UTILITIES("فواتير"),
        EQUIPMENT("معدات"),
        MAINTENANCE("صيانة"),
        SUPPLIES("مستلزمات"),
        OTHER_EXPENSE("مصروف آخر");

        private final String arabicName;

        TransactionCategory(String arabicName) {
            this.arabicName = arabicName;
        }

        public String getArabicName() {
            return arabicName;
        }
    }

    public enum PaymentMethod {
        CASH("كاش"),
        CARD("بطاقة"),
        BANK_TRANSFER("تحويل بنكي"),
        MOBILE_WALLET("محفظة إلكترونية");

        private final String arabicName;

        PaymentMethod(String arabicName) {
            this.arabicName = arabicName;
        }

        public String getArabicName() {
            return arabicName;
        }
    }
}