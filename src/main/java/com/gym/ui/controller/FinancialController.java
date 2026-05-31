package com.gym.ui.controller;

import com.gym.entity.*;
import com.gym.service.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class FinancialController {

    // Summary Labels
    @FXML private Label lblTotalIncome;
    @FXML private Label lblTotalExpenses;
    @FXML private Label lblNetProfit;
    @FXML private Label lblProfitMargin;
    @FXML private Label lblTransactionCount;
    @FXML private Label lblPeriodRange;
    @FXML private Label lblDaysCount;
    @FXML private Label lblDailyAverage;

    // Income breakdown mini labels
    @FXML private Label lblSubscriptionIncome;
    @FXML private Label lblRenewalIncome;
    @FXML private Label lblSalesIncome;

    // Expense breakdown mini labels
    @FXML private Label lblSalaryExpense;
    @FXML private Label lblRentExpense;
    @FXML private Label lblOtherExpense;

    // Totals
    @FXML private Label lblIncomeTotal;
    @FXML private Label lblExpenseTotal;
    @FXML private Label lblTableSummary;

    // Date pickers
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    // Table
    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colCategory;
    @FXML private TableColumn<Transaction, String> colDescription;
    @FXML private TableColumn<Transaction, String> colAmount;
    @FXML private TableColumn<Transaction, String> colBalance;

    // Filters and search
    @FXML private ComboBox<String> filterType;
    @FXML private TextField searchField;

    // Breakdown containers
    @FXML private VBox incomeBreakdown;
    @FXML private VBox expenseBreakdown;
    @FXML private VBox quickStats;

    // Root container - add this to your FXML as the main container
    @FXML private BorderPane rootContainer;

    @Autowired
    private FinancialService financialService;

    @Autowired
    private PdfExportService pdfExportService;

    @Autowired
    private AdminService adminService;

    private final ObservableList<Transaction> transactionsList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private List<Transaction> allTransactions;
    private boolean isPasswordVerified = false;

    @FXML
    public void initialize() {
        // تعطيل جميع عناصر الصفحة في البداية
        disableAllControls();

        // التحقق من كلمة المرور أولاً
        if (!showPasswordVerificationDialog()) {
            // إذا لم يقم المستخدم بإدخال كلمة المرور الصحيحة، نبقي الصفحة معطلة
            showUnauthorizedMessage();
            return;
        }

        isPasswordVerified = true;
        enableAllControls();
        setupDatePickers();
        setupTable();
        setupFilters();
        setupSearch();
        loadData();
    }

    private void disableAllControls() {
        if (rootContainer != null) {
            rootContainer.setDisable(true);
        }
        // تعطيل جميع العناصر الأساسية
        if (startDatePicker != null) startDatePicker.setDisable(true);
        if (endDatePicker != null) endDatePicker.setDisable(true);
        if (transactionsTable != null) transactionsTable.setDisable(true);
        if (filterType != null) filterType.setDisable(true);
        if (searchField != null) searchField.setDisable(true);
    }

    private void enableAllControls() {
        if (rootContainer != null) {
            rootContainer.setDisable(false);
        }
        if (startDatePicker != null) startDatePicker.setDisable(false);
        if (endDatePicker != null) endDatePicker.setDisable(false);
        if (transactionsTable != null) transactionsTable.setDisable(false);
        if (filterType != null) filterType.setDisable(false);
        if (searchField != null) searchField.setDisable(false);
    }

    private void showUnauthorizedMessage() {
        // إظهار رسالة في وسط الصفحة
        if (rootContainer != null) {
            VBox messageBox = new VBox(20);
            messageBox.setAlignment(Pos.CENTER);
            messageBox.setStyle("-fx-background-color: #1a1a1a;");

            Label icon = new Label("🔒");
            icon.setStyle("-fx-font-size: 72px;");

            Label title = new Label("غير مصرح");
            title.setStyle("-fx-font-size: 24px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");

            Label message = new Label("ليس لديك صلاحية للوصول إلى هذه الصفحة");
            message.setStyle("-fx-font-size: 16px; -fx-text-fill: #9ca3af;");

            messageBox.getChildren().addAll(icon, title, message);
            rootContainer.setCenter(messageBox);
        }
    }

    private boolean showPasswordVerificationDialog() {
        while (true) {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("التحقق من الهوية");
            dialog.setHeaderText("يرجى إدخال كلمة المرور للوصول إلى الإدارة المالية");
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

            ButtonType loginButtonType = new ButtonType("تسجيل الدخول", ButtonBar.ButtonData.OK_DONE);
            ButtonType changePasswordButtonType = new ButtonType("تغيير كلمة المرور", ButtonBar.ButtonData.OTHER);
            dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, changePasswordButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("أدخل كلمة المرور");

            grid.add(new Label("كلمة المرور:"), 0, 0);
            grid.add(passwordField, 1, 0);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == loginButtonType) {
                    return passwordField.getText();
                } else if (dialogButton == changePasswordButtonType) {
                    return "CHANGE_PASSWORD";
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();

            // إذا ضغط المستخدم Cancel أو أغلق النافذة
            if (result.isEmpty()) {
                return false;
            }

            String value = result.get();

            // إذا اختار تغيير كلمة المرور
            if ("CHANGE_PASSWORD".equals(value)) {
                if (showChangePasswordDialog()) {
                    showSuccess("تم بنجاح", "تم تغيير كلمة المرور بنجاح. يرجى تسجيل الدخول بكلمة المرور الجديدة.");
                }
                // نكمل اللوب ونعرض نافذة تسجيل الدخول مرة أخرى
                continue;
            }

            // التحقق من كلمة المرور
            if (value == null || value.trim().isEmpty()) {
                showError("خطأ", "يرجى إدخال كلمة المرور");
                continue;
            }

            if (adminService.verifyPassword(value)) {
                return true;
            } else {
                showError("خطأ", "كلمة المرور غير صحيحة!");
                continue;
            }
        }
    }

    private boolean showChangePasswordDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("تغيير كلمة المرور");
        dialog.setHeaderText("تغيير كلمة مرور الإدارة المالية");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("أدخل كلمة المرور القديمة");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("أدخل كلمة المرور الجديدة");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("أعد إدخال كلمة المرور الجديدة");

        grid.add(new Label("كلمة المرور القديمة:"), 0, 0);
        grid.add(oldPasswordField, 1, 0);
        grid.add(new Label("كلمة المرور الجديدة:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("تأكيد كلمة المرور:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String oldPassword = oldPasswordField.getText();
                String newPassword = newPasswordField.getText();
                String confirmPassword = confirmPasswordField.getText();

                // التحقق من أن جميع الحقول ممتلئة
                if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    showError("خطأ", "يرجى ملء جميع الحقول");
                    return false;
                }

                // التحقق من طول كلمة المرور الجديدة
                if (newPassword.length() < 4) {
                    showError("خطأ", "يجب أن تكون كلمة المرور الجديدة 4 أحرف على الأقل");
                    return false;
                }

                // محاولة تغيير كلمة المرور
                boolean success = adminService.changePassword(oldPassword, newPassword, confirmPassword);

                if (success) {
                    return true;
                } else {
                    // تحديد سبب الفشل
                    if (!adminService.verifyPassword(oldPassword)) {
                        showError("خطأ", "كلمة المرور القديمة غير صحيحة");
                    } else if (!newPassword.equals(confirmPassword)) {
                        showError("خطأ", "كلمة المرور الجديدة وتأكيد كلمة المرور غير متطابقتين");
                    } else if (newPassword.equals(oldPassword)) {
                        showError("خطأ", "كلمة المرور الجديدة يجب أن تكون مختلفة عن القديمة");
                    } else {
                        showError("خطأ", "فشل في تغيير كلمة المرور");
                    }
                    return false;
                }
            }
            return false;
        });

        Optional<Boolean> result = dialog.showAndWait();
        return result.orElse(false);
    }

    private void setupDatePickers() {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today);

        startDatePicker.setOnAction(e -> {
            if (isPasswordVerified) {
                loadData();
            }
        });
        endDatePicker.setOnAction(e -> {
            if (isPasswordVerified) {
                loadData();
            }
        });
    }

    private void setupTable() {
        colDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTransactionDate().format(dateFormatter)));

        colType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getType().getArabicName()));

        colType.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("إيراد")) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colCategory.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCategory().getArabicName()));

        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        colAmount.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatCurrency(cellData.getValue().getAmount())));

        colAmount.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Transaction t = getTableView().getItems().get(getIndex());
                    if (t.getType() == Transaction.TransactionType.INCOME) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Running balance column
        colBalance.setCellValueFactory(cellData -> {
            int index = transactionsList.indexOf(cellData.getValue());
            BigDecimal balance = calculateRunningBalance(index);
            return new SimpleStringProperty(formatCurrency(balance));
        });

        colBalance.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1D546D;");
                }
            }
        });

        transactionsTable.setItems(transactionsList);
    }

    private BigDecimal calculateRunningBalance(int upToIndex) {
        BigDecimal balance = BigDecimal.ZERO;
        for (int i = transactionsList.size() - 1; i >= upToIndex; i--) {
            Transaction t = transactionsList.get(i);
            if (t.getType() == Transaction.TransactionType.INCOME) {
                balance = balance.add(t.getAmount());
            } else {
                balance = balance.subtract(t.getAmount());
            }
        }
        return balance;
    }

    private void setupFilters() {
        filterType.setItems(FXCollections.observableArrayList("الكل", "الإيرادات", "المصروفات"));
        filterType.setValue("الكل");
        filterType.setOnAction(e -> {
            if (isPasswordVerified) {
                applyFilter();
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, newVal) -> {
            if (isPasswordVerified) {
                applyFilter();
            }
        });
    }

    private void loadData() {
        if (!isPasswordVerified) {
            return;
        }

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        FinancialService.FinancialSummary summary = financialService.getFinancialSummary(start, end);
        allTransactions = summary.transactions();

        // Update main summary labels
        lblTotalIncome.setText(formatCurrency(summary.totalIncome()));
        lblTotalExpenses.setText(formatCurrency(summary.totalExpenses()));
        lblNetProfit.setText(formatCurrency(summary.netProfit()));

        // Profit styling
        if (summary.netProfit().compareTo(BigDecimal.ZERO) >= 0) {
            lblNetProfit.getStyleClass().removeAll("expense-value");
            lblNetProfit.getStyleClass().add("profit-value");
        } else {
            lblNetProfit.getStyleClass().removeAll("profit-value");
            lblNetProfit.getStyleClass().add("expense-value");
        }

        // Profit margin calculation
        if (summary.totalIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = summary.netProfit()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(summary.totalIncome(), 1, RoundingMode.HALF_UP);
            lblProfitMargin.setText(margin + "%");
        } else {
            lblProfitMargin.setText("0%");
        }

        // Transaction count
        lblTransactionCount.setText(String.valueOf(summary.transactions().size()));

        // Period info
        lblPeriodRange.setText(start.format(dateFormatter) + " - " + end.format(dateFormatter));
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        lblDaysCount.setText(days + " يوم");

        // Daily average
        if (days > 0) {
            BigDecimal dailyAvg = summary.netProfit().divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
            lblDailyAverage.setText(formatCurrency(dailyAvg));
        }

        // Update mini breakdowns
        updateMiniBreakdowns(summary);

        // Update totals
        lblIncomeTotal.setText(formatCurrency(summary.totalIncome()));
        lblExpenseTotal.setText(formatCurrency(summary.totalExpenses()));

        // Update transactions
        transactionsList.clear();
        transactionsList.addAll(summary.transactions());

        // Update table summary
        lblTableSummary.setText("إجمالي المعاملات: " + summary.transactions().size() +
                " | إيرادات: " + formatCurrency(summary.totalIncome()) +
                " | مصروفات: " + formatCurrency(summary.totalExpenses()));

        // Update detailed breakdowns
        updateBreakdowns(summary);

        // Update quick stats
        updateQuickStats(summary, days);
    }

    private void updateMiniBreakdowns(FinancialService.FinancialSummary summary) {
        // Income breakdown
        BigDecimal subscriptions = summary.incomeBreakdown().getOrDefault(Transaction.TransactionCategory.SUBSCRIPTION, BigDecimal.ZERO);
        BigDecimal renewals = summary.incomeBreakdown().getOrDefault(Transaction.TransactionCategory.RENEWAL, BigDecimal.ZERO);
        BigDecimal sales = summary.incomeBreakdown().getOrDefault(Transaction.TransactionCategory.PRODUCT_SALE, BigDecimal.ZERO);

        lblSubscriptionIncome.setText(formatShortCurrency(subscriptions));
        lblRenewalIncome.setText(formatShortCurrency(renewals));
        lblSalesIncome.setText(formatShortCurrency(sales));

        // Expense breakdown
        BigDecimal salary = summary.expenseBreakdown().getOrDefault(Transaction.TransactionCategory.SALARY, BigDecimal.ZERO);
        BigDecimal rent = summary.expenseBreakdown().getOrDefault(Transaction.TransactionCategory.RENT, BigDecimal.ZERO);

        BigDecimal other = BigDecimal.ZERO;
        for (Map.Entry<Transaction.TransactionCategory, BigDecimal> entry : summary.expenseBreakdown().entrySet()) {
            if (entry.getKey() != Transaction.TransactionCategory.SALARY &&
                    entry.getKey() != Transaction.TransactionCategory.RENT) {
                other = other.add(entry.getValue());
            }
        }

        lblSalaryExpense.setText(formatShortCurrency(salary));
        lblRentExpense.setText(formatShortCurrency(rent));
        lblOtherExpense.setText(formatShortCurrency(other));
    }

    private void updateBreakdowns(FinancialService.FinancialSummary summary) {
        incomeBreakdown.getChildren().clear();
        summary.incomeBreakdown().forEach((category, amount) -> {
            HBox row = createBreakdownRow(category.getArabicName(), amount, summary.totalIncome(), true);
            incomeBreakdown.getChildren().add(row);
        });

        if (incomeBreakdown.getChildren().isEmpty()) {
            Label empty = new Label("لا توجد إيرادات في هذه الفترة");
            empty.getStyleClass().add("empty-label");
            incomeBreakdown.getChildren().add(empty);
        }

        expenseBreakdown.getChildren().clear();
        summary.expenseBreakdown().forEach((category, amount) -> {
            HBox row = createBreakdownRow(category.getArabicName(), amount, summary.totalExpenses(), false);
            expenseBreakdown.getChildren().add(row);
        });

        if (expenseBreakdown.getChildren().isEmpty()) {
            Label empty = new Label("لا توجد مصروفات في هذه الفترة");
            empty.getStyleClass().add("empty-label");
            expenseBreakdown.getChildren().add(empty);
        }
    }

    private HBox createBreakdownRow(String category, BigDecimal amount, BigDecimal total, boolean isIncome) {
        HBox row = new HBox(10);
        row.getStyleClass().add("breakdown-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Category label
        Label categoryLabel = new Label(category);
        categoryLabel.getStyleClass().add("breakdown-category");
        categoryLabel.setMinWidth(100);

        // Progress bar
        ProgressBar progressBar = new ProgressBar();
        progressBar.getStyleClass().add(isIncome ? "income-progress" : "expense-progress");
        progressBar.setPrefWidth(80);
        progressBar.setMaxHeight(8);

        if (total.compareTo(BigDecimal.ZERO) > 0) {
            double progress = amount.divide(total, 4, RoundingMode.HALF_UP).doubleValue();
            progressBar.setProgress(progress);
        } else {
            progressBar.setProgress(0);
        }

        // Percentage
        String percentage = "0%";
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            percentage = amount.multiply(BigDecimal.valueOf(100))
                    .divide(total, 0, RoundingMode.HALF_UP) + "%";
        }
        Label percentLabel = new Label(percentage);
        percentLabel.getStyleClass().add("breakdown-percent");
        percentLabel.setMinWidth(35);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Amount
        Label amountLabel = new Label(formatCurrency(amount));
        amountLabel.getStyleClass().addAll("breakdown-amount", isIncome ? "text-success" : "text-danger");

        row.getChildren().addAll(categoryLabel, progressBar, percentLabel, spacer, amountLabel);
        return row;
    }

    private void updateQuickStats(FinancialService.FinancialSummary summary, long days) {
        quickStats.getChildren().clear();

        // Highest income day
        addQuickStat("أعلى إيراد", getMaxTransaction(summary.transactions(), Transaction.TransactionType.INCOME));
        addQuickStat("أعلى مصروف", getMaxTransaction(summary.transactions(), Transaction.TransactionType.EXPENSE));

        // Average per transaction
        if (!summary.transactions().isEmpty()) {
            BigDecimal avgIncome = summary.totalIncome()
                    .divide(BigDecimal.valueOf(Math.max(1, countByType(summary.transactions(), Transaction.TransactionType.INCOME))), 0, RoundingMode.HALF_UP);
            BigDecimal avgExpense = summary.totalExpenses()
                    .divide(BigDecimal.valueOf(Math.max(1, countByType(summary.transactions(), Transaction.TransactionType.EXPENSE))), 0, RoundingMode.HALF_UP);

            addQuickStat("متوسط الإيراد", formatCurrency(avgIncome));
            addQuickStat("متوسط المصروف", formatCurrency(avgExpense));
        }
    }

    private String getMaxTransaction(List<Transaction> transactions, Transaction.TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .max(BigDecimal::compareTo)
                .map(this::formatCurrency)
                .orElse("0 ج.م");
    }

    private long countByType(List<Transaction> transactions, Transaction.TransactionType type) {
        return transactions.stream().filter(t -> t.getType() == type).count();
    }

    private void addQuickStat(String label, String value) {
        HBox stat = new HBox(10);
        stat.setAlignment(Pos.CENTER_LEFT);
        stat.getStyleClass().add("quick-stat-row");

        Label labelNode = new Label(label + ":");
        labelNode.getStyleClass().add("quick-stat-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("quick-stat-value");

        stat.getChildren().addAll(labelNode, spacer, valueNode);
        quickStats.getChildren().add(stat);
    }

    private void applyFilter() {
        if (!isPasswordVerified) {
            return;
        }

        String filter = filterType.getValue();
        String search = searchField.getText().toLowerCase();

        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> {
                    boolean matchesType = switch (filter) {
                        case "الإيرادات" -> t.getType() == Transaction.TransactionType.INCOME;
                        case "المصروفات" -> t.getType() == Transaction.TransactionType.EXPENSE;
                        default -> true;
                    };

                    boolean matchesSearch = search.isEmpty() ||
                            t.getDescription().toLowerCase().contains(search) ||
                            t.getCategory().getArabicName().toLowerCase().contains(search);

                    return matchesType && matchesSearch;
                })
                .toList();

        transactionsList.clear();
        transactionsList.addAll(filtered);
    }

    @FXML
    private void showAddExpenseDialog() {
        if (!isPasswordVerified) {
            showError("خطأ", "ليس لديك صلاحية للقيام بهذا الإجراء");
            return;
        }

        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle("إضافة مصروف");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<Transaction.TransactionCategory> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList(
                Arrays.stream(Transaction.TransactionCategory.values())
                        .filter(c -> c.name().contains("EXPENSE") ||
                                c == Transaction.TransactionCategory.SALARY ||
                                c == Transaction.TransactionCategory.RENT ||
                                c == Transaction.TransactionCategory.UTILITIES ||
                                c == Transaction.TransactionCategory.EQUIPMENT ||
                                c == Transaction.TransactionCategory.MAINTENANCE ||
                                c == Transaction.TransactionCategory.SUPPLIES)
                        .toList()
        ));
        categoryCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Transaction.TransactionCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getArabicName());
            }
        });
        categoryCombo.setButtonCell(categoryCombo.getCellFactory().call(null));

        TextField amountField = new TextField();
        amountField.setPromptText("المبلغ");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("الوصف");

        TextArea notesField = new TextArea();
        notesField.setPromptText("ملاحظات");
        notesField.setPrefRowCount(2);

        grid.add(new Label("التصنيف:"), 0, 0);
        grid.add(categoryCombo, 1, 0);
        grid.add(new Label("المبلغ:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("الوصف:"), 0, 2);
        grid.add(descriptionField, 1, 2);
        grid.add(new Label("ملاحظات:"), 0, 3);
        grid.add(notesField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (categoryCombo.getValue() == null || amountField.getText().isEmpty() || descriptionField.getText().isEmpty()) {
                    showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
                    return null;
                }

                try {
                    return financialService.createExpense(
                            categoryCombo.getValue(),
                            new BigDecimal(amountField.getText()),
                            descriptionField.getText(),
                            notesField.getText()
                    );
                } catch (NumberFormatException e) {
                    showError("خطأ", "يرجى إدخال مبلغ صحيح");
                    return null;
                }
            }
            return null;
        });

        Optional<Transaction> result = dialog.showAndWait();
        result.ifPresent(t -> {
            loadData();
            showSuccess("تم بنجاح", "تم إضافة المصروف");
        });
    }

    @FXML
    private void showAddIncomeDialog() {
        if (!isPasswordVerified) {
            showError("خطأ", "ليس لديك صلاحية للقيام بهذا الإجراء");
            return;
        }

        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle("إضافة إيراد");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<Transaction.TransactionCategory> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList(
                Transaction.TransactionCategory.OTHER_INCOME,
                Transaction.TransactionCategory.PRODUCT_SALE,
                Transaction.TransactionCategory.SUBSCRIPTION
        ));
        categoryCombo.setValue(Transaction.TransactionCategory.OTHER_INCOME);
        categoryCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Transaction.TransactionCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getArabicName());
            }
        });
        categoryCombo.setButtonCell(categoryCombo.getCellFactory().call(null));

        TextField amountField = new TextField();
        amountField.setPromptText("المبلغ");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("الوصف");

        TextArea notesField = new TextArea();
        notesField.setPromptText("ملاحظات");
        notesField.setPrefRowCount(2);

        grid.add(new Label("التصنيف:"), 0, 0);
        grid.add(categoryCombo, 1, 0);
        grid.add(new Label("المبلغ:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("الوصف:"), 0, 2);
        grid.add(descriptionField, 1, 2);
        grid.add(new Label("ملاحظات:"), 0, 3);
        grid.add(notesField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (categoryCombo.getValue() == null || amountField.getText().isEmpty() || descriptionField.getText().isEmpty()) {
                    showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
                    return null;
                }

                try {
                    return financialService.createIncome(
                            categoryCombo.getValue(),
                            new BigDecimal(amountField.getText()),
                            descriptionField.getText(),
                            notesField.getText()
                    );
                } catch (NumberFormatException e) {
                    showError("خطأ", "يرجى إدخال مبلغ صحيح");
                    return null;
                }
            }
            return null;
        });

        Optional<Transaction> result = dialog.showAndWait();
        result.ifPresent(t -> {
            loadData();
            showSuccess("تم بنجاح", "تم إضافة الإيراد");
        });
    }

    @FXML
    private void exportDetailedPdf() {
        if (!isPasswordVerified) {
            showError("خطأ", "ليس لديك صلاحية للقيام بهذا الإجراء");
            return;
        }

        try {
            List<Transaction> filteredTransactions = new java.util.ArrayList<>(transactionsList);

            File pdf = pdfExportService.exportDetailedFinancialReportFiltered(
                    startDatePicker.getValue(),
                    endDatePicker.getValue(),
                    filteredTransactions,
                    filterType.getValue(),
                    "نظام إدارة الجيم"
            );
            showSuccess("تم التصدير بنجاح", "تم حفظ التقرير المفصل في:\n" + pdf.getAbsolutePath());
        } catch (Exception e) {
            showError("خطأ في التصدير", e.getMessage());
        }
    }

    @FXML
    private void exportSummaryPdf() {
        if (!isPasswordVerified) {
            showError("خطأ", "ليس لديك صلاحية للقيام بهذا الإجراء");
            return;
        }

        try {
            List<Transaction> filteredTransactions = new java.util.ArrayList<>(transactionsList);

            File pdf = pdfExportService.exportFinancialReportFiltered(
                    startDatePicker.getValue(),
                    endDatePicker.getValue(),
                    filteredTransactions,
                    filterType.getValue(),
                    "نظام إدارة الجيم"
            );
            showSuccess("تم التصدير بنجاح", "تم حفظ التقرير في:\n" + pdf.getAbsolutePath());
        } catch (Exception e) {
            showError("خطأ في التصدير", e.getMessage());
        }
    }

    @FXML
    private void showTodayReport() {
        if (!isPasswordVerified) {
            return;
        }
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);
        loadData();
    }

    @FXML
    private void showWeekReport() {
        if (!isPasswordVerified) {
            return;
        }
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.minusDays(6));
        endDatePicker.setValue(today);
        loadData();
    }

    @FXML
    private void showMonthReport() {
        if (!isPasswordVerified) {
            return;
        }
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today);
        loadData();
    }

    @FXML
    private void showYearReport() {
        if (!isPasswordVerified) {
            return;
        }
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfYear(1));
        endDatePicker.setValue(today);
        loadData();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ج.م";
        return String.format("%,.0f ج.م", amount);
    }

    private String formatShortCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        if (amount.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return String.format("%.1f ألف", amount.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP));
        }
        return String.format("%,.0f", amount);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        alert.showAndWait();
    }
}