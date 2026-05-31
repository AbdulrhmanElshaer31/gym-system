package com.gym.repository;

import com.gym.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    List<InventoryLog> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("SELECT il FROM InventoryLog il WHERE il.createdAt BETWEEN :start AND :end ORDER BY il.createdAt DESC")
    List<InventoryLog> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT il FROM InventoryLog il WHERE il.product.id = :productId AND il.createdAt BETWEEN :start AND :end ORDER BY il.createdAt DESC")
    List<InventoryLog> findByProductAndDateRange(@Param("productId") Long productId,
                                                   @Param("start") LocalDateTime start, 
                                                   @Param("end") LocalDateTime end);

    @Query("SELECT il.type, COALESCE(SUM(il.quantityChange), 0) FROM InventoryLog il WHERE il.product.id = :productId AND il.createdAt BETWEEN :start AND :end GROUP BY il.type")
    List<Object[]> sumChangesByType(@Param("productId") Long productId,
                                     @Param("start") LocalDateTime start, 
                                     @Param("end") LocalDateTime end);
}
