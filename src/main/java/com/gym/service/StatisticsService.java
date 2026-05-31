package com.gym.service;

import com.gym.entity.Transaction;
import com.gym.repository.MemberRepository;
import com.gym.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for advanced statistics and analytics.
 * Provides growth rates, comparisons, and business intelligence metrics.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Calculate revenue growth rate between two periods
     */
    public GrowthRate getRevenueGrowthRate(LocalDate currentStart, LocalDate currentEnd,
                                           LocalDate previousStart, LocalDate previousEnd) {
        BigDecimal currentRevenue = transactionRepository.sumIncomeByDateRange(currentStart, currentEnd);
        BigDecimal previousRevenue = transactionRepository.sumIncomeByDateRange(previousStart, previousEnd);
        
        if (currentRevenue == null) currentRevenue = BigDecimal.ZERO;
        if (previousRevenue == null) previousRevenue = BigDecimal.ZERO;
        
        double growthRate = calculateGrowthPercentage(previousRevenue, currentRevenue);
        
        return new GrowthRate(previousRevenue, currentRevenue, growthRate);
    }

    /**
     * Get month-over-month comparison
     */
    public MonthlyComparison getMonthlyComparison() {
        LocalDate today = LocalDate.now();
        
        // Current month
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate currentMonthEnd = today;
        
        // Previous month
        LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDate previousMonthEnd = currentMonthStart.minusDays(1);
        
        // Income
        BigDecimal currentIncome = transactionRepository.sumIncomeByDateRange(currentMonthStart, currentMonthEnd);
        BigDecimal previousIncome = transactionRepository.sumIncomeByDateRange(previousMonthStart, previousMonthEnd);
        
        if (currentIncome == null) currentIncome = BigDecimal.ZERO;
        if (previousIncome == null) previousIncome = BigDecimal.ZERO;
        
        // Expenses
        BigDecimal currentExpenses = transactionRepository.sumExpensesByDateRange(currentMonthStart, currentMonthEnd);
        BigDecimal previousExpenses = transactionRepository.sumExpensesByDateRange(previousMonthStart, previousMonthEnd);
        
        if (currentExpenses == null) currentExpenses = BigDecimal.ZERO;
        if (previousExpenses == null) previousExpenses = BigDecimal.ZERO;
        
        // Members
        long currentActiveMembers = memberRepository.countActiveMembers(today);
        long previousActiveMembers = memberRepository.countActiveMembers(previousMonthEnd);
        
        // Growth rates
        double incomeGrowth = calculateGrowthPercentage(previousIncome, currentIncome);
        double expenseGrowth = calculateGrowthPercentage(previousExpenses, currentExpenses);
        double memberGrowth = previousActiveMembers == 0 ? 100.0 : 
                ((double)(currentActiveMembers - previousActiveMembers) / previousActiveMembers) * 100;
        
        // Net profit
        BigDecimal currentProfit = currentIncome.subtract(currentExpenses);
        BigDecimal previousProfit = previousIncome.subtract(previousExpenses);
        double profitGrowth = calculateGrowthPercentage(previousProfit, currentProfit);
        
        return new MonthlyComparison(
                currentIncome, previousIncome, incomeGrowth,
                currentExpenses, previousExpenses, expenseGrowth,
                currentProfit, previousProfit, profitGrowth,
                currentActiveMembers, previousActiveMembers, memberGrowth
        );
    }

    /**
     * Calculate average revenue per member
     */
    public BigDecimal getAverageRevenuePerMember() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        
        BigDecimal monthlyIncome = transactionRepository.sumIncomeByDateRange(monthStart, today);
        long activeMembers = memberRepository.countActiveMembers(today);
        
        if (monthlyIncome == null || activeMembers == 0) {
            return BigDecimal.ZERO;
        }
        
        return monthlyIncome.divide(BigDecimal.valueOf(activeMembers), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate subscription renewal rate
     */
    public double getRenewalRate(LocalDate start, LocalDate end) {
        // Count renewals in period
        long renewals = transactionRepository.findByTypeAndDateRange(
                Transaction.TransactionType.INCOME, start, end)
                .stream()
                .filter(t -> t.getCategory() == Transaction.TransactionCategory.RENEWAL)
                .count();
        
        // Count expired members in period (potential renewals)
        long expiredMembers = memberRepository.findExpiredMembers(start).size();
        
        if (expiredMembers == 0) {
            return 100.0;
        }
        
        return ((double) renewals / expiredMembers) * 100;
    }

    /**
     * Get daily average income for a period
     */
    public BigDecimal getDailyAverageIncome(LocalDate start, LocalDate end) {
        BigDecimal totalIncome = transactionRepository.sumIncomeByDateRange(start, end);
        if (totalIncome == null) return BigDecimal.ZERO;
        
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        return totalIncome.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    /**
     * Get daily average expenses for a period
     */
    public BigDecimal getDailyAverageExpenses(LocalDate start, LocalDate end) {
        BigDecimal totalExpenses = transactionRepository.sumExpensesByDateRange(start, end);
        if (totalExpenses == null) return BigDecimal.ZERO;
        
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        return totalExpenses.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    /**
     * Get income breakdown by category with percentages
     */
    public Map<Transaction.TransactionCategory, CategoryBreakdown> getIncomeBreakdown(LocalDate start, LocalDate end) {
        BigDecimal totalIncome = transactionRepository.sumIncomeByDateRange(start, end);
        if (totalIncome == null || totalIncome.equals(BigDecimal.ZERO)) {
            return new HashMap<>();
        }
        
        Map<Transaction.TransactionCategory, CategoryBreakdown> breakdown = new HashMap<>();
        
        transactionRepository.sumByCategory(Transaction.TransactionType.INCOME, start, end)
                .forEach(row -> {
                    Transaction.TransactionCategory category = (Transaction.TransactionCategory) row[0];
                    BigDecimal amount = (BigDecimal) row[1];
                    double percentage = amount.divide(totalIncome, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
                    breakdown.put(category, new CategoryBreakdown(amount, percentage));
                });
        
        return breakdown;
    }

    /**
     * Get expense breakdown by category with percentages
     */
    public Map<Transaction.TransactionCategory, CategoryBreakdown> getExpenseBreakdown(LocalDate start, LocalDate end) {
        BigDecimal totalExpenses = transactionRepository.sumExpensesByDateRange(start, end);
        if (totalExpenses == null || totalExpenses.equals(BigDecimal.ZERO)) {
            return new HashMap<>();
        }
        
        Map<Transaction.TransactionCategory, CategoryBreakdown> breakdown = new HashMap<>();
        
        transactionRepository.sumByCategory(Transaction.TransactionType.EXPENSE, start, end)
                .forEach(row -> {
                    Transaction.TransactionCategory category = (Transaction.TransactionCategory) row[0];
                    BigDecimal amount = (BigDecimal) row[1];
                    double percentage = amount.divide(totalExpenses, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
                    breakdown.put(category, new CategoryBreakdown(amount, percentage));
                });
        
        return breakdown;
    }

    /**
     * Get comprehensive dashboard statistics
     */
    public DashboardStats getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);
        
        // Member stats
        long activeMembers = memberRepository.countActiveMembers(today);
        long expiredMembers = memberRepository.countExpiredMembers(today);
        long nearExpiryMembers = memberRepository.findMembersNearExpiry(today, today.plusDays(7)).size();
        
        // Financial stats
        BigDecimal dailyIncome = transactionRepository.sumDailyIncome(today);
        BigDecimal dailyExpenses = transactionRepository.sumDailyExpenses(today);
        BigDecimal monthlyIncome = transactionRepository.sumIncomeByDateRange(monthStart, today);
        BigDecimal monthlyExpenses = transactionRepository.sumExpensesByDateRange(monthStart, today);
        BigDecimal yearlyIncome = transactionRepository.sumIncomeByDateRange(yearStart, today);
        BigDecimal yearlyExpenses = transactionRepository.sumExpensesByDateRange(yearStart, today);
        
        // Null safety
        if (dailyIncome == null) dailyIncome = BigDecimal.ZERO;
        if (dailyExpenses == null) dailyExpenses = BigDecimal.ZERO;
        if (monthlyIncome == null) monthlyIncome = BigDecimal.ZERO;
        if (monthlyExpenses == null) monthlyExpenses = BigDecimal.ZERO;
        if (yearlyIncome == null) yearlyIncome = BigDecimal.ZERO;
        if (yearlyExpenses == null) yearlyExpenses = BigDecimal.ZERO;
        
        // Comparisons
        MonthlyComparison comparison = getMonthlyComparison();
        BigDecimal avgRevenuePerMember = getAverageRevenuePerMember();
        
        return new DashboardStats(
                activeMembers, expiredMembers, nearExpiryMembers,
                dailyIncome, dailyExpenses, dailyIncome.subtract(dailyExpenses),
                monthlyIncome, monthlyExpenses, monthlyIncome.subtract(monthlyExpenses),
                yearlyIncome, yearlyExpenses, yearlyIncome.subtract(yearlyExpenses),
                comparison.incomeGrowth(), comparison.memberGrowth(),
                avgRevenuePerMember
        );
    }

    /**
     * Helper method to calculate growth percentage
     */
    private double calculateGrowthPercentage(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.equals(BigDecimal.ZERO)) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    // Record classes for structured data
    
    public record GrowthRate(BigDecimal previousValue, BigDecimal currentValue, double growthPercentage) {}
    
    public record CategoryBreakdown(BigDecimal amount, double percentage) {}
    
    public record MonthlyComparison(
            BigDecimal currentIncome, BigDecimal previousIncome, double incomeGrowth,
            BigDecimal currentExpenses, BigDecimal previousExpenses, double expenseGrowth,
            BigDecimal currentProfit, BigDecimal previousProfit, double profitGrowth,
            long currentMembers, long previousMembers, double memberGrowth
    ) {}
    
    public record DashboardStats(
            long activeMembers, long expiredMembers, long nearExpiryMembers,
            BigDecimal dailyIncome, BigDecimal dailyExpenses, BigDecimal dailyProfit,
            BigDecimal monthlyIncome, BigDecimal monthlyExpenses, BigDecimal monthlyProfit,
            BigDecimal yearlyIncome, BigDecimal yearlyExpenses, BigDecimal yearlyProfit,
            double incomeGrowthRate, double memberGrowthRate,
            BigDecimal avgRevenuePerMember
    ) {}
}
