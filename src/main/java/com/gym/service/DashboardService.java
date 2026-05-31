package com.gym.service;

import com.gym.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberService memberService;
    private final PlanService planService;
    private final FinancialService financialService;
    private final ProductService productService;

    public DashboardStats getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        Long activeMembers = memberService.countActiveMembers();
        Long expiredMembers = memberService.countExpiredMembers();
        List<Member> nearExpiryMembers = memberService.getMembersNearExpiry();
        
        BigDecimal dailyIncome = financialService.getDailyIncome();
        BigDecimal monthlyIncome = financialService.getMonthlyIncome();
        BigDecimal monthlyExpenses = financialService.getMonthlyExpenses();
        BigDecimal monthlyProfit = monthlyIncome.subtract(monthlyExpenses);
        
        Plan mostPopularPlan = planService.getMostPopularPlan();
        Map<Long, Long> membersByPlan = planService.getMembersCountByPlan();
        
        List<Product> lowStockProducts = productService.getLowStockProducts();
        BigDecimal inventoryValue = productService.calculateTotalInventoryValue();

        Map<Transaction.TransactionCategory, BigDecimal> incomeByCategory = 
                financialService.getIncomeByCategory(startOfMonth, today);
        Map<Transaction.TransactionCategory, BigDecimal> expensesByCategory = 
                financialService.getExpensesByCategory(startOfMonth, today);

        return new DashboardStats(
                activeMembers,
                expiredMembers,
                nearExpiryMembers.size(),
                dailyIncome,
                monthlyIncome,
                monthlyExpenses,
                monthlyProfit,
                mostPopularPlan,
                membersByPlan,
                lowStockProducts,
                inventoryValue,
                incomeByCategory,
                expensesByCategory
        );
    }

    public record DashboardStats(
            Long activeMembers,
            Long expiredMembers,
            int nearExpiryCount,
            BigDecimal dailyIncome,
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpenses,
            BigDecimal monthlyProfit,
            Plan mostPopularPlan,
            Map<Long, Long> membersByPlan,
            List<Product> lowStockProducts,
            BigDecimal inventoryValue,
            Map<Transaction.TransactionCategory, BigDecimal> incomeByCategory,
            Map<Transaction.TransactionCategory, BigDecimal> expensesByCategory
    ) {}
}
