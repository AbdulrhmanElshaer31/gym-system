package com.gym.service;

import com.gym.entity.Transaction;
import com.gym.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Transaction> getTransactionsByDateRange(LocalDate start, LocalDate end) {
        return transactionRepository.findByDateRange(start, end);
    }

    public List<Transaction> getTodayTransactions() {
        return transactionRepository.findByDate(LocalDate.now());
    }

    @Transactional
    public Transaction createExpense(Transaction.TransactionCategory category, 
                                      BigDecimal amount, 
                                      String description, 
                                      String notes) {
        log.info("Creating expense: category={}, amount={}", category, amount);
        try {
            Transaction transaction = new Transaction();
            transaction.setType(Transaction.TransactionType.EXPENSE);
            transaction.setCategory(category);
            transaction.setAmount(amount);
            transaction.setDescription(description);
            transaction.setNotes(notes);
            transaction.setTransactionDate(LocalDate.now());
            transaction.setVersion(0L);
            Transaction saved = transactionRepository.save(transaction);
            log.debug("Expense created with id: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Failed to create expense", e);
            throw e;
        }
    }

    @Transactional
    public Transaction createIncome(Transaction.TransactionCategory category, 
                                     BigDecimal amount, 
                                     String description, 
                                     String notes) {
        log.info("Creating income: category={}, amount={}", category, amount);
        try {
            Transaction transaction = new Transaction();
            transaction.setType(Transaction.TransactionType.INCOME);
            transaction.setCategory(category);
            transaction.setAmount(amount);
            transaction.setDescription(description);
            transaction.setNotes(notes);
            transaction.setTransactionDate(LocalDate.now());
            transaction.setVersion(0L);
            Transaction saved = transactionRepository.save(transaction);
            log.debug("Income created with id: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Failed to create income", e);
            throw e;
        }
    }

    public BigDecimal getDailyIncome() {
        return transactionRepository.sumDailyIncome(LocalDate.now());
    }

    public BigDecimal getDailyExpenses() {
        return transactionRepository.sumDailyExpenses(LocalDate.now());
    }

    public BigDecimal getMonthlyIncome() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        return transactionRepository.sumIncomeByDateRange(startOfMonth, LocalDate.now());
    }

    public BigDecimal getMonthlyExpenses() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        return transactionRepository.sumExpensesByDateRange(startOfMonth, LocalDate.now());
    }

    public BigDecimal getIncomeByRange(LocalDate start, LocalDate end) {
        return transactionRepository.sumIncomeByDateRange(start, end);
    }

    public BigDecimal getExpensesByRange(LocalDate start, LocalDate end) {
        return transactionRepository.sumExpensesByDateRange(start, end);
    }

    public BigDecimal getNetProfit(LocalDate start, LocalDate end) {
        BigDecimal income = getIncomeByRange(start, end);
        BigDecimal expenses = getExpensesByRange(start, end);
        return income.subtract(expenses);
    }

    public Map<Transaction.TransactionCategory, BigDecimal> getIncomeByCategory(LocalDate start, LocalDate end) {
        Map<Transaction.TransactionCategory, BigDecimal> result = new HashMap<>();
        transactionRepository.sumByCategory(Transaction.TransactionType.INCOME, start, end)
                .forEach(row -> {
                    result.put((Transaction.TransactionCategory) row[0], (BigDecimal) row[1]);
                });
        return result;
    }

    public Map<Transaction.TransactionCategory, BigDecimal> getExpensesByCategory(LocalDate start, LocalDate end) {
        Map<Transaction.TransactionCategory, BigDecimal> result = new HashMap<>();
        transactionRepository.sumByCategory(Transaction.TransactionType.EXPENSE, start, end)
                .forEach(row -> {
                    result.put((Transaction.TransactionCategory) row[0], (BigDecimal) row[1]);
                });
        return result;
    }

    public FinancialSummary getFinancialSummary(LocalDate start, LocalDate end) {
        BigDecimal totalIncome = getIncomeByRange(start, end);
        BigDecimal totalExpenses = getExpensesByRange(start, end);
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);
        
        Map<Transaction.TransactionCategory, BigDecimal> incomeBreakdown = getIncomeByCategory(start, end);
        Map<Transaction.TransactionCategory, BigDecimal> expenseBreakdown = getExpensesByCategory(start, end);
        
        List<Transaction> transactions = getTransactionsByDateRange(start, end);
        
        return new FinancialSummary(totalIncome, totalExpenses, netProfit, 
                                     incomeBreakdown, expenseBreakdown, transactions, start, end);
    }

    public record FinancialSummary(
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal netProfit,
            Map<Transaction.TransactionCategory, BigDecimal> incomeBreakdown,
            Map<Transaction.TransactionCategory, BigDecimal> expenseBreakdown,
            List<Transaction> transactions,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
