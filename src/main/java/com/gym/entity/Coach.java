package com.gym.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coaches", indexes = {
        @Index(name = "idx_coach_name", columnList = "name"),
        @Index(name = "idx_coach_deleted", columnList = "deleted")
})
@DynamicUpdate
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @NotBlank(message = "اسم المدرب مطلوب")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "التخصص مطلوب")
    @Column(nullable = false, length = 500)
    private String specialty; // مثلاً: تدريب القوة، اليوجا، إلخ

    @DecimalMin(value = "0.0", message = "الراتب يجب أن يكون صفر أو أكثر")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salary;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "coach", fetch = FetchType.LAZY)
    private List<Member> members = new ArrayList<>();

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
}