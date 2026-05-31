package com.gym.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogType type;

    @Column(nullable = false)
    private Integer quantityBefore;

    @Column(nullable = false)
    private Integer quantityChange;

    @Column(nullable = false)
    private Integer quantityAfter;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum LogType {
        SALE("بيع"),
        PURCHASE("شراء"),
        ADJUSTMENT("تعديل"),
        DAMAGE("تالف"),
        RETURN("مرتجع");

        private final String arabicName;

        LogType(String arabicName) {
            this.arabicName = arabicName;
        }

        public String getArabicName() {
            return arabicName;
        }
    }
}
