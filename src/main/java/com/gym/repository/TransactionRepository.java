package com.gym.repository;

import com.gym.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Eager fetch member and product to avoid LazyInitializationException
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.member LEFT JOIN FETCH t.product ORDER BY t.createdAt DESC")
    List<Transaction> findAllByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.member LEFT JOIN FETCH t.product WHERE t.type = :type ORDER BY t.createdAt DESC")
    List<Transaction> findByTypeOrderByCreatedAtDesc(@Param("type") Transaction.TransactionType type);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.member LEFT JOIN FETCH t.product WHERE t.transactionDate BETWEEN :start AND :end ORDER BY t.transactionDate DESC")
    List<Transaction> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.member LEFT JOIN FETCH t.product WHERE t.transactionDate = :date ORDER BY t.createdAt DESC")
    List<Transaction> findByDate(@Param("date") LocalDate date);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.member LEFT JOIN FETCH t.product WHERE t.type = :type AND t.transactionDate BETWEEN :start AND :end ORDER BY t.transactionDate DESC")
    List<Transaction> findByTypeAndDateRange(@Param("type") Transaction.TransactionType type,
                                              @Param("start") LocalDate start, 
                                              @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'INCOME' AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumIncomeByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'EXPENSE' AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumExpensesByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'INCOME' AND t.transactionDate = :date")
    BigDecimal sumDailyIncome(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'EXPENSE' AND t.transactionDate = :date")
    BigDecimal sumDailyExpenses(@Param("date") LocalDate date);

    @Query("SELECT t.category, COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND t.transactionDate BETWEEN :start AND :end GROUP BY t.category")
    List<Object[]> sumByCategory(@Param("type") Transaction.TransactionType type,
                                  @Param("start") LocalDate start, 
                                  @Param("end") LocalDate end);
}
