package com.gym.service;

import com.gym.entity.*;
import com.gym.exception.InsufficientStockException;
import com.gym.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final TransactionRepository transactionRepository;

    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> searchProducts(String search) {
        if (search == null || search.trim().isEmpty()) {
            return getAllActiveProducts();
        }
        return productRepository.searchProducts(search.trim());
    }

    public List<Product> getProductsByCategory(Product.ProductCategory category) {
        return productRepository.findByCategory(category);
    }

    @Transactional
    public Product createProduct(Product product) {
        product.setActive(true);
        ensureVersion(product);
        Product savedProduct = productRepository.save(product);
        
        if (product.getQuantity() > 0) {
            logInventoryChange(savedProduct, InventoryLog.LogType.PURCHASE, 
                    0, product.getQuantity(), "إضافة منتج جديد");
        }
        
        return savedProduct;
    }

    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        return productRepository.findById(id)
                .map(product -> {
                    ensureVersion(product);
                    product.setName(updatedProduct.getName());
                    product.setCategory(updatedProduct.getCategory());
                    product.setPurchasePrice(updatedProduct.getPurchasePrice());
                    product.setSalePrice(updatedProduct.getSalePrice());
                    product.setDescription(updatedProduct.getDescription());
                    product.setLowStockThreshold(updatedProduct.getLowStockThreshold());
                    return productRepository.save(product);
                })
                .orElseThrow(() -> new RuntimeException("المنتج غير موجود"));
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("المنتج غير موجود"));
        ensureVersion(product);
        product.setActive(false);
        productRepository.save(product);
        productRepository.flush(); // Force flush to ensure changes are committed
    }

    @Transactional
    public Product sellProduct(Long productId, int quantity) {
        log.info("Attempting to sell product {} with quantity {}", productId, quantity);
        
        // Use pessimistic locking to prevent race conditions
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> {
                    log.error("Product not found: {}", productId);
                    return new RuntimeException("المنتج غير موجود");
                });
        
        if (product.getQuantity() < quantity) {
            log.warn("Insufficient stock for product {}: available={}, requested={}", 
                    productId, product.getQuantity(), quantity);
            throw new InsufficientStockException("الكمية المطلوبة غير متوفرة");
        }
        
        int previousQuantity = product.getQuantity();
        product.setQuantity(previousQuantity - quantity);
        Product savedProduct = productRepository.save(product);
        productRepository.flush();
        
        log.debug("Product {} sold: {} units", productId, quantity);
        
        // Log inventory change
        logInventoryChange(savedProduct, InventoryLog.LogType.SALE, 
                previousQuantity, -quantity, "بيع منتج");
        
        // Create financial transaction
        BigDecimal saleAmount = savedProduct.getSalePrice().multiply(BigDecimal.valueOf(quantity));
        Transaction transaction = new Transaction();
        transaction.setType(Transaction.TransactionType.INCOME);
        transaction.setCategory(Transaction.TransactionCategory.PRODUCT_SALE);
        transaction.setAmount(saleAmount);
        transaction.setDescription("بيع " + quantity + " × " + savedProduct.getName());
        transaction.setProduct(savedProduct);
        transaction.setTransactionDate(LocalDate.now());
        transactionRepository.save(transaction);
        
        return savedProduct;
    }

    @Transactional
    public Product addStock(Long productId, int quantity, BigDecimal purchaseCost) {
        log.info("Adding stock to product {}: quantity={}", productId, quantity);
        
        // Use pessimistic locking to prevent race conditions
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> {
                    log.error("Product not found: {}", productId);
                    return new RuntimeException("المنتج غير موجود");
                });
        
        int previousQuantity = product.getQuantity();
        product.setQuantity(previousQuantity + quantity);
        Product savedProduct = productRepository.save(product);
        productRepository.flush();
        
        log.debug("Stock added to product {}: new quantity={}", productId, savedProduct.getQuantity());
        
        // Log inventory change
        logInventoryChange(savedProduct, InventoryLog.LogType.PURCHASE, 
                previousQuantity, quantity, "شراء مخزون جديد");
        
        // Create expense transaction if cost provided
        if (purchaseCost != null && purchaseCost.compareTo(BigDecimal.ZERO) > 0) {
            Transaction transaction = new Transaction();
            transaction.setType(Transaction.TransactionType.EXPENSE);
            transaction.setCategory(Transaction.TransactionCategory.SUPPLIES);
            transaction.setAmount(purchaseCost);
            transaction.setDescription("شراء " + quantity + " × " + savedProduct.getName());
            transaction.setProduct(savedProduct);
            transaction.setTransactionDate(LocalDate.now());
            transactionRepository.save(transaction);
        }
        
        return savedProduct;
    }

    @Transactional
    public Product adjustStock(Long productId, int newQuantity, String reason) {
        log.info("Adjusting stock for product {}: newQuantity={}", productId, newQuantity);
        
        // Use pessimistic locking to prevent race conditions
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> {
                    log.error("Product not found: {}", productId);
                    return new RuntimeException("المنتج غير موجود");
                });
        
        int previousQuantity = product.getQuantity();
        int change = newQuantity - previousQuantity;
        product.setQuantity(newQuantity);
        Product savedProduct = productRepository.save(product);
        productRepository.flush();
        
        log.debug("Stock adjusted for product {}: change={}", productId, change);
        
        // Log inventory change
        logInventoryChange(savedProduct, InventoryLog.LogType.ADJUSTMENT, 
                previousQuantity, change, reason);
        
        return savedProduct;
    }

    private void ensureVersion(Product product) {
        if (product.getVersion() == null) {
            product.setVersion(0L);
        }
    }

    private void logInventoryChange(Product product, InventoryLog.LogType type, 
                                     int quantityBefore, int change, String reason) {
        InventoryLog log = new InventoryLog();
        log.setProduct(product);
        log.setType(type);
        log.setQuantityBefore(quantityBefore);
        log.setQuantityChange(change);
        log.setQuantityAfter(quantityBefore + change);
        log.setReason(reason);
        inventoryLogRepository.save(log);
    }

    public List<InventoryLog> getProductHistory(Long productId) {
        return inventoryLogRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public List<InventoryLog> getInventoryLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return inventoryLogRepository.findByDateRange(start, end);
    }

    /**
     * Calculate total inventory value using optimized database query
     */
    public BigDecimal calculateTotalInventoryValue() {
        return productRepository.calculateTotalInventoryValue();
    }

    /**
     * Calculate potential revenue using optimized database query
     */
    public BigDecimal calculatePotentialRevenue() {
        return productRepository.calculatePotentialRevenue();
    }

    /**
     * Count low stock products
     */
    public long countLowStockProducts() {
        return productRepository.countLowStockProducts();
    }
}
