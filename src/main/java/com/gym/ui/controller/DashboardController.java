package com.gym.ui.controller;

import com.gym.entity.*;
import com.gym.service.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

@Component
public class DashboardController {

    @FXML private Label lblActiveMembers;
    @FXML private Label lblExpiredMembers;
    @FXML private Label lblNearExpiry;
    @FXML private Label lblDailyIncome;
    @FXML private Label lblMonthlyIncome;
    @FXML private Label lblMonthlyExpenses;
    @FXML private Label lblMonthlyProfit;
    @FXML private Label lblMostPopularPlan;
    @FXML private Label lblInventoryValue;
    
    @FXML private PieChart planDistributionChart;
    @FXML private BarChart<String, Number> incomeChart;
    @FXML private VBox alertsContainer;
    @FXML private VBox lowStockContainer;

    @Autowired
    private DashboardService dashboardService;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ar", "EG"));

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    public void loadDashboardData() {
        Platform.runLater(() -> {
            try {
                DashboardService.DashboardStats stats = dashboardService.getDashboardStats();
                
                // Update member stats
                lblActiveMembers.setText(String.valueOf(stats.activeMembers()));
                lblExpiredMembers.setText(String.valueOf(stats.expiredMembers()));
                lblNearExpiry.setText(String.valueOf(stats.nearExpiryCount()));
                
                // Update financial stats
                lblDailyIncome.setText(formatCurrency(stats.dailyIncome()));
                lblMonthlyIncome.setText(formatCurrency(stats.monthlyIncome()));
                lblMonthlyExpenses.setText(formatCurrency(stats.monthlyExpenses()));
                lblMonthlyProfit.setText(formatCurrency(stats.monthlyProfit()));
                
                // Style profit based on positive/negative
                if (stats.monthlyProfit().compareTo(BigDecimal.ZERO) >= 0) {
                    lblMonthlyProfit.getStyleClass().removeAll("text-danger");
                    lblMonthlyProfit.getStyleClass().add("text-success");
                } else {
                    lblMonthlyProfit.getStyleClass().removeAll("text-success");
                    lblMonthlyProfit.getStyleClass().add("text-danger");
                }
                
                // Update plan info
                if (stats.mostPopularPlan() != null) {
                    lblMostPopularPlan.setText(stats.mostPopularPlan().getName());
                } else {
                    lblMostPopularPlan.setText("لا توجد بيانات");
                }
                
                // Update inventory stats
                if (lblInventoryValue != null) {
                    lblInventoryValue.setText(formatCurrency(stats.inventoryValue()));
                }
                
                // Update charts
                updatePlanChart(stats.membersByPlan());
                updateIncomeChart(stats.incomeByCategory());
                
                // Update alerts
                updateAlerts(stats);
                
                // Update low stock list
                updateLowStockList(stats);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updatePlanChart(Map<Long, Long> membersByPlan) {
        planDistributionChart.getData().clear();
        
        membersByPlan.forEach((planId, count) -> {
            // In real implementation, fetch plan name
            PieChart.Data slice = new PieChart.Data("خطة " + planId, count);
            planDistributionChart.getData().add(slice);
        });
    }

    private void updateIncomeChart(Map<Transaction.TransactionCategory, BigDecimal> incomeByCategory) {
        incomeChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("الإيرادات");
        
        incomeByCategory.forEach((category, amount) -> {
            series.getData().add(new XYChart.Data<>(category.getArabicName(), amount));
        });
        
        incomeChart.getData().add(series);
    }

    private void updateAlerts(DashboardService.DashboardStats stats) {
        alertsContainer.getChildren().clear();
        
        // Near expiry alert
        if (stats.nearExpiryCount() > 0) {
            HBox alert = createAlert(
                "⚠️ " + stats.nearExpiryCount() + " اشتراكات تنتهي خلال أسبوع",
                "warning"
            );
            alertsContainer.getChildren().add(alert);
        }
        
        // Expired members alert
        if (stats.expiredMembers() > 0) {
            HBox alert = createAlert(
                "❌ " + stats.expiredMembers() + " اشتراكات منتهية",
                "danger"
            );
            alertsContainer.getChildren().add(alert);
        }
        
        // Low stock alert
        if (!stats.lowStockProducts().isEmpty()) {
            HBox alert = createAlert(
                "📦 " + stats.lowStockProducts().size() + " منتجات تحتاج إعادة تخزين",
                "warning"
            );
            alertsContainer.getChildren().add(alert);
        }
        
        // Profit status
        if (stats.monthlyProfit().compareTo(BigDecimal.ZERO) < 0) {
            HBox alert = createAlert(
                "💰 تحذير: خسارة هذا الشهر " + formatCurrency(stats.monthlyProfit().abs()),
                "danger"
            );
            alertsContainer.getChildren().add(alert);
        }
        
        if (alertsContainer.getChildren().isEmpty()) {
            Label noAlerts = new Label("✅ لا توجد تنبيهات");
            noAlerts.getStyleClass().add("no-alerts-label");
            alertsContainer.getChildren().add(noAlerts);
        }
    }

    private void updateLowStockList(DashboardService.DashboardStats stats) {
        lowStockContainer.getChildren().clear();
        
        for (Product product : stats.lowStockProducts()) {
            HBox item = new HBox(10);
            item.getStyleClass().add("low-stock-item");
            
            Label name = new Label(product.getName());
            name.getStyleClass().add("product-name");
            
            Label qty = new Label("المتبقي: " + product.getQuantity());
            qty.getStyleClass().add("product-qty");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            item.getChildren().addAll(name, spacer, qty);
            lowStockContainer.getChildren().add(item);
        }
        
        if (stats.lowStockProducts().isEmpty()) {
            Label noLowStock = new Label("✅ جميع المنتجات متوفرة");
            noLowStock.getStyleClass().add("no-low-stock-label");
            lowStockContainer.getChildren().add(noLowStock);
        }
    }

    private HBox createAlert(String message, String type) {
        HBox alert = new HBox(10);
        alert.getStyleClass().addAll("alert", "alert-" + type);
        
        Label label = new Label(message);
        label.getStyleClass().add("alert-text");
        
        alert.getChildren().add(label);
        return alert;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ج.م";
        return String.format("%.0f ج.م", amount);
    }

    @FXML
    private void refreshDashboard() {
        loadDashboardData();
    }
}
