package com.gym.repository;

import com.gym.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    List<Product> findByActiveTrueOrderByNameAsc();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.quantity <= p.lowStockThreshold")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category = :category ORDER BY p.name")
    List<Product> findByCategory(@Param("category") Product.ProductCategory category);

    @Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Product> searchProducts(@Param("search") String search);

    boolean existsByName(String name);

    /**
     * Calculate total inventory value directly in the database
     */
    @Query("SELECT COALESCE(SUM(p.purchasePrice * p.quantity), 0) FROM Product p WHERE p.active = true")
    BigDecimal calculateTotalInventoryValue();

    /**
     * Calculate potential revenue directly in the database
     */
    @Query("SELECT COALESCE(SUM(p.salePrice * p.quantity), 0) FROM Product p WHERE p.active = true")
    BigDecimal calculatePotentialRevenue();

    /**
     * Count products by category
     */
    @Query("SELECT p.category, COUNT(p) FROM Product p WHERE p.active = true GROUP BY p.category")
    List<Object[]> countByCategory();

    /**
     * Count low stock products
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.quantity <= p.lowStockThreshold")
    long countLowStockProducts();

    /**
     * Find product with pessimistic write lock to prevent race conditions
     * This ensures exclusive access to the product during concurrent operations
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
}
