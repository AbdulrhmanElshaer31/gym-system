package com.gym.service;

import com.gym.entity.AuditLog;
import com.gym.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing audit logs.
 * Tracks all changes to entities for compliance and debugging.
 */
@Service
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log a create action
     */
    @Transactional
    public void logCreate(String entityType, Long entityId, Object newValue, String description) {
        createAuditLog(entityType, entityId, AuditLog.AuditAction.CREATE, null, convertToString(newValue), description);
    }

    /**
     * Log an update action
     */
    @Transactional
    public void logUpdate(String entityType, Long entityId, Object oldValue, Object newValue, String description) {
        createAuditLog(entityType, entityId, AuditLog.AuditAction.UPDATE, convertToString(oldValue), convertToString(newValue), description);
    }

    /**
     * Log a delete action
     */
    @Transactional
    public void logDelete(String entityType, Long entityId, Object oldValue, String description) {
        createAuditLog(entityType, entityId, AuditLog.AuditAction.DELETE, convertToString(oldValue), null, description);
    }

    /**
     * Log a soft delete action
     */
    @Transactional
    public void logSoftDelete(String entityType, Long entityId, String description) {
        createAuditLog(entityType, entityId, AuditLog.AuditAction.SOFT_DELETE, null, null, description);
    }

    /**
     * Log a subscription renewal
     */
    @Transactional
    public void logRenewal(Long memberId, Object oldValue, Object newValue, String description) {
        createAuditLog("Member", memberId, AuditLog.AuditAction.RENEW, convertToString(oldValue), convertToString(newValue), description);
    }

    /**
     * Log a product sale
     */
    @Transactional
    public void logSale(Long productId, int quantity, String description) {
        createAuditLog("Product", productId, AuditLog.AuditAction.SELL, null, "quantity: " + quantity, description);
    }

    /**
     * Log stock addition
     */
    @Transactional
    public void logStockAdd(Long productId, int quantity, String description) {
        createAuditLog("Product", productId, AuditLog.AuditAction.STOCK_ADD, null, "quantity: " + quantity, description);
    }

    /**
     * Log stock adjustment
     */
    @Transactional
    public void logStockAdjust(Long productId, int oldQuantity, int newQuantity, String reason) {
        createAuditLog("Product", productId, AuditLog.AuditAction.STOCK_ADJUST, 
                "quantity: " + oldQuantity, "quantity: " + newQuantity, reason);
    }

    /**
     * Log a generic action
     */
    @Transactional
    public void logAction(String entityType, Long entityId, AuditLog.AuditAction action, 
                          Object oldValue, Object newValue, String description) {
        createAuditLog(entityType, entityId, action, convertToString(oldValue), convertToString(newValue), description);
    }

    /**
     * Convert object to string representation
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Map) {
            return value.toString();
        }
        // Simple toString for other objects
        return value.toString();
    }

    /**
     * Create an audit log entry
     */
    private void createAuditLog(String entityType, Long entityId, AuditLog.AuditAction action,
                                String oldValue, String newValue, String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .description(description)
                    .timestamp(LocalDateTime.now())
                    .performedBy("System")
                    .build();
            
            auditLogRepository.save(auditLog);
            log.info("Audit log created: {} {} on {} - {}", action, entityType, entityId, description);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    /**
     * Get entity history
     */
    public List<AuditLog> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    /**
     * Get recent activity with pagination
     */
    public Page<AuditLog> getRecentActivity(int page, int size) {
        return auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }

    /**
     * Get recent logs (limited)
     */
    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findRecentLogs(limit);
    }

    /**
     * Get logs by date range
     */
    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByDateRange(start, end);
    }

    /**
     * Get action statistics for a period
     */
    public Map<AuditLog.AuditAction, Long> getActionStatistics(LocalDateTime start, LocalDateTime end) {
        Map<AuditLog.AuditAction, Long> stats = new HashMap<>();
        auditLogRepository.countActionsByType(start, end).forEach(row -> {
            stats.put((AuditLog.AuditAction) row[0], (Long) row[1]);
        });
        return stats;
    }

    /**
     * Cleanup old audit logs (scheduled daily at 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldLogs() {
        // Keep logs for 90 days
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        auditLogRepository.deleteByTimestampBefore(cutoff);
        log.info("Cleaned up audit logs older than {}", cutoff);
    }
}
