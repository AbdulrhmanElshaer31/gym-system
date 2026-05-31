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
@Table(name = "products", indexes = {
    @Index(name = "idx_product_category", columnList = "category"),
    @Index(name = "idx_product_active", columnList = "active"),
    @Index(name = "idx_product_quantity", columnList = "quantity")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version = 0L; // Optimistic locking - initialized to avoid NPE

    @NotBlank(message = "اسم المنتج مطلوب")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "التصنيف مطلوب")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @NotNull(message = "سعر الشراء مطلوب")
    @Min(value = 0, message = "سعر الشراء يجب أن يكون صفر أو أكثر")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @NotNull(message = "سعر البيع مطلوب")
    @Min(value = 0, message = "سعر البيع يجب أن يكون صفر أو أكثر")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;

    @NotNull(message = "الكمية مطلوبة")
    @Min(value = 0, message = "الكمية يجب أن تكون صفر أو أكثر")
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer lowStockThreshold = 5;

    @Column(length = 500)
    private String description;

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

    @Transient
    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }

    @Transient
    public BigDecimal getProfitPerUnit() {
        return salePrice.subtract(purchasePrice);
    }

    public enum ProductCategory {
        BEVERAGE("مشروبات"),
        FOOD("مأكولات"),
        SUPPLEMENT("مكملات غذائية"),
        ACCESSORY("إكسسوارات"),
        OTHER("أخرى");

        private final String arabicName;

        ProductCategory(String arabicName) {
            this.arabicName = arabicName;
        }

        public String getArabicName() {
            return arabicName;
        }
    }
}
