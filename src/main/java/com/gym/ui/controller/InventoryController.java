package com.gym.ui.controller;

import com.gym.entity.*;
import com.gym.service.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class InventoryController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    
    @FXML private Label lblTotalSales;
    @FXML private Label lblTotalPurchases;
    @FXML private Label lblNetChange;
    @FXML private Label lblCurrentValue;
    
    @FXML private TableView<InventoryLog> logsTable;
    @FXML private TableColumn<InventoryLog, String> colDate;
    @FXML private TableColumn<InventoryLog, String> colProduct;
    @FXML private TableColumn<InventoryLog, String> colType;
    @FXML private TableColumn<InventoryLog, String> colBefore;
    @FXML private TableColumn<InventoryLog, String> colChange;
    @FXML private TableColumn<InventoryLog, String> colAfter;
    @FXML private TableColumn<InventoryLog, String> colReason;
    
    @FXML private VBox discrepancyList;

    @Autowired
    private ProductService productService;
    
    @Autowired
    private FinancialService financialService;
    
    @Autowired
    private PdfExportService pdfExportService;

    private final ObservableList<InventoryLog> logsList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupDatePickers();
        setupTable();
        loadData();
    }

    private void setupDatePickers() {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today);
        
        startDatePicker.setOnAction(e -> loadData());
        endDatePicker.setOnAction(e -> loadData());
    }

    private void setupTable() {
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCreatedAt().format(dateFormatter)));
        
        colProduct.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getProduct().getName()));
        
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
                    InventoryLog log = getTableView().getItems().get(getIndex());
                    switch (log.getType()) {
                        case SALE:
                            setStyle("-fx-text-fill: #ef4444;");
                            break;
                        case PURCHASE:
                            setStyle("-fx-text-fill: #22c55e;");
                            break;
                        case ADJUSTMENT:
                            setStyle("-fx-text-fill: #f59e0b;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        colBefore.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf(cellData.getValue().getQuantityBefore())));
        
        colChange.setCellValueFactory(cellData -> {
            int change = cellData.getValue().getQuantityChange();
            return new SimpleStringProperty((change >= 0 ? "+" : "") + change);
        });
        
        colChange.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("+")) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        colAfter.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf(cellData.getValue().getQuantityAfter())));
        
        colReason.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getReason() != null ? cellData.getValue().getReason() : "-"));
        
        logsTable.setItems(logsList);
    }

    private void loadData() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
        
        // Load logs
        List<InventoryLog> logs = productService.getInventoryLogsByDateRange(startDateTime, endDateTime);
        logsList.clear();
        logsList.addAll(logs);
        
        // Calculate summary
        calculateSummary(logs);
        
        // Check for discrepancies
        checkDiscrepancies();
    }

    private void calculateSummary(List<InventoryLog> logs) {
        int totalSales = 0;
        int totalPurchases = 0;
        
        for (InventoryLog log : logs) {
            if (log.getType() == InventoryLog.LogType.SALE) {
                totalSales += Math.abs(log.getQuantityChange());
            } else if (log.getType() == InventoryLog.LogType.PURCHASE) {
                totalPurchases += log.getQuantityChange();
            }
        }
        
        int netChange = totalPurchases - totalSales;
        
        lblTotalSales.setText(totalSales + " وحدة");
        lblTotalPurchases.setText(totalPurchases + " وحدة");
        
        lblNetChange.setText((netChange >= 0 ? "+" : "") + netChange + " وحدة");
        if (netChange >= 0) {
            lblNetChange.setStyle("-fx-text-fill: #22c55e;");
        } else {
            lblNetChange.setStyle("-fx-text-fill: #ef4444;");
        }
        
        BigDecimal currentValue = productService.calculateTotalInventoryValue();
        lblCurrentValue.setText(currentValue + " ج.م");
    }

    private void checkDiscrepancies() {
        discrepancyList.getChildren().clear();
        
        List<Product> products = productService.getAllActiveProducts();
        boolean hasDiscrepancies = false;
        
        for (Product product : products) {
            // Check for unusual patterns
            List<InventoryLog> productLogs = productService.getProductHistory(product.getId());
            
            if (!productLogs.isEmpty()) {
                InventoryLog lastLog = productLogs.get(0);
                
                // Check if current quantity doesn't match last log's after quantity
                if (product.getQuantity() != lastLog.getQuantityAfter()) {
                    hasDiscrepancies = true;
                    addDiscrepancyItem(product.getName(), 
                        "الكمية الحالية (" + product.getQuantity() + ") لا تطابق آخر سجل (" + lastLog.getQuantityAfter() + ")");
                }
            }
            
            // Check for negative or suspicious patterns
            int adjustments = (int) productLogs.stream()
                .filter(l -> l.getType() == InventoryLog.LogType.ADJUSTMENT)
                .count();
            
            if (adjustments > 5) {
                hasDiscrepancies = true;
                addDiscrepancyItem(product.getName(), 
                    "عدد تعديلات كبير (" + adjustments + ") - يرجى المراجعة");
            }
        }
        
        if (!hasDiscrepancies) {
            Label noDiscrepancies = new Label("✅ لا توجد تناقضات في الجرد");
            noDiscrepancies.getStyleClass().add("no-discrepancies");
            discrepancyList.getChildren().add(noDiscrepancies);
        }
    }

    private void addDiscrepancyItem(String productName, String issue) {
        HBox item = new HBox(10);
        item.getStyleClass().add("discrepancy-item");
        item.setPadding(new Insets(10));
        
        Label icon = new Label("⚠️");
        Label nameLabel = new Label(productName);
        nameLabel.getStyleClass().add("discrepancy-product");
        Label issueLabel = new Label(issue);
        issueLabel.getStyleClass().add("discrepancy-issue");
        issueLabel.setWrapText(true);
        
        VBox textBox = new VBox(5);
        textBox.getChildren().addAll(nameLabel, issueLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        
        item.getChildren().addAll(icon, textBox);
        discrepancyList.getChildren().add(item);
    }

    @FXML
    private void showAdjustmentDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("تعديل المخزون");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        
        ButtonType adjustButtonType = new ButtonType("تعديل", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adjustButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        ComboBox<Product> productCombo = new ComboBox<>();
        productCombo.setItems(FXCollections.observableArrayList(productService.getAllActiveProducts()));
        productCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (الحالي: " + item.getQuantity() + ")");
            }
        });
        productCombo.setButtonCell(productCombo.getCellFactory().call(null));
        
        Spinner<Integer> newQuantitySpinner = new Spinner<>(0, 10000, 0);
        newQuantitySpinner.setEditable(true);
        
        TextField reasonField = new TextField();
        reasonField.setPromptText("سبب التعديل");
        
        productCombo.setOnAction(e -> {
            if (productCombo.getValue() != null) {
                newQuantitySpinner.getValueFactory().setValue(productCombo.getValue().getQuantity());
            }
        });
        
        grid.add(new Label("المنتج:"), 0, 0);
        grid.add(productCombo, 1, 0);
        grid.add(new Label("الكمية الجديدة:"), 0, 1);
        grid.add(newQuantitySpinner, 1, 1);
        grid.add(new Label("السبب:"), 0, 2);
        grid.add(reasonField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adjustButtonType) {
                if (productCombo.getValue() == null || reasonField.getText().isEmpty()) {
                    showError("خطأ", "يرجى ملء جميع الحقول");
                    return null;
                }
                
                productService.adjustStock(
                    productCombo.getValue().getId(),
                    newQuantitySpinner.getValue(),
                    reasonField.getText()
                );
                
                loadData();
                showSuccess("تم بنجاح", "تم تعديل المخزون");
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    @FXML
    private void exportToPdf() {
        try {
            File pdf = pdfExportService.exportInventoryReport(
                startDatePicker.getValue(),
                endDatePicker.getValue(),
                "GYM Management System"
            );
            showSuccess("تم التصدير", "تم حفظ تقرير الجرد في:\n" + pdf.getAbsolutePath());
        } catch (Exception e) {
            showError("خطأ في التصدير", e.getMessage());
        }
    }

    @FXML
    private void refreshData() {
        loadData();
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
