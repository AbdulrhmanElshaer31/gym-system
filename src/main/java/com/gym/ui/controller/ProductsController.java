package com.gym.ui.controller;

import com.gym.entity.*;
import com.gym.service.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class ProductsController {

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Long> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Integer> colQuantity;
    @FXML private TableColumn<Product, String> colPurchasePrice;
    @FXML private TableColumn<Product, String> colSalePrice;
    @FXML private TableColumn<Product, String> colProfit;
    @FXML private TableColumn<Product, Void> colActions;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCategory;
    @FXML private VBox lowStockAlerts;
    @FXML private Label lblTotalValue;
    @FXML private Label lblPotentialRevenue;

    @Autowired
    private ProductService productService;

    private final ObservableList<Product> productsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadProducts();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        colCategory.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCategory().getArabicName()));
        
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQuantity.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    Product product = getTableView().getItems().get(getIndex());
                    if (product.isLowStock()) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #22c55e;");
                    }
                }
            }
        });
        
        colPurchasePrice.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getPurchasePrice() + " ج.م"));
        
        colSalePrice.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSalePrice() + " ج.م"));
        
        colProfit.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getProfitPerUnit() + " ج.م"));
        colProfit.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                }
            }
        });
        
        setupActionsColumn();
        
        productsTable.setItems(productsList);
    }

    private void setupActionsColumn() {
        Callback<TableColumn<Product, Void>, TableCell<Product, Void>> cellFactory = param -> new TableCell<>() {
            private final MenuButton menuButton = new MenuButton("⋮");
            
            {
                menuButton.getStyleClass().add("action-menu");
                
                MenuItem sellItem = new MenuItem("بيع");
                sellItem.setOnAction(e -> showSellDialog(getTableView().getItems().get(getIndex())));
                
                MenuItem addStockItem = new MenuItem("إضافة مخزون");
                addStockItem.setOnAction(e -> showAddStockDialog(getTableView().getItems().get(getIndex())));
                
                MenuItem editItem = new MenuItem("تعديل");
                editItem.setOnAction(e -> editProduct(getTableView().getItems().get(getIndex())));
                
                MenuItem historyItem = new MenuItem("سجل الحركات");
                historyItem.setOnAction(e -> showProductHistory(getTableView().getItems().get(getIndex())));
                
                MenuItem deleteItem = new MenuItem("حذف");
                deleteItem.setOnAction(e -> deleteProduct(getTableView().getItems().get(getIndex())));
                deleteItem.getStyleClass().add("danger-menu-item");
                
                menuButton.getItems().addAll(sellItem, addStockItem, new SeparatorMenuItem(), 
                                              editItem, historyItem, new SeparatorMenuItem(), deleteItem);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : menuButton);
            }
        };
        
        colActions.setCellFactory(cellFactory);
    }

    private void setupFilters() {
        filterCategory.setItems(FXCollections.observableArrayList(
            "الكل", "مشروبات", "مأكولات", "مكملات غذائية", "إكسسوارات", "أخرى"
        ));
        filterCategory.setValue("الكل");
        filterCategory.setOnAction(e -> applyFilters());
        
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void loadProducts() {
        productsList.clear();
        productsList.addAll(productService.getAllActiveProducts());
        updateSummary();
        updateLowStockAlerts();
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase();
        String category = filterCategory.getValue();
        
        List<Product> allProducts = productService.getAllActiveProducts();
        
        productsList.clear();
        productsList.addAll(allProducts.stream()
            .filter(p -> {
                boolean matchesSearch = search.isEmpty() || 
                    p.getName().toLowerCase().contains(search);
                
                boolean matchesCategory = category.equals("الكل") ||
                    p.getCategory().getArabicName().equals(category);
                
                return matchesSearch && matchesCategory;
            })
            .toList());
    }

    private void updateSummary() {
        BigDecimal totalValue = productService.calculateTotalInventoryValue();
        BigDecimal potentialRevenue = productService.calculatePotentialRevenue();
        
        lblTotalValue.setText(totalValue + " ج.م");
        lblPotentialRevenue.setText(potentialRevenue + " ج.م");
    }

    private void updateLowStockAlerts() {
        lowStockAlerts.getChildren().clear();
        
        List<Product> lowStock = productService.getLowStockProducts();
        
        for (Product product : lowStock) {
            HBox alert = new HBox(10);
            alert.getStyleClass().add("low-stock-alert");
            
            Label name = new Label("⚠️ " + product.getName());
            name.getStyleClass().add("alert-name");
            
            Label qty = new Label("المتبقي: " + product.getQuantity());
            qty.getStyleClass().add("alert-qty");
            
            Button addBtn = new Button("إضافة مخزون");
            addBtn.getStyleClass().add("small-button");
            addBtn.setOnAction(e -> showAddStockDialog(product));
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            alert.getChildren().addAll(name, qty, spacer, addBtn);
            lowStockAlerts.getChildren().add(alert);
        }
        
        if (lowStock.isEmpty()) {
            Label noAlerts = new Label("✅ جميع المنتجات متوفرة بكميات كافية");
            noAlerts.getStyleClass().add("no-alerts");
            lowStockAlerts.getChildren().add(noAlerts);
        }
    }

    @FXML
    private void showAddProductDialog() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("إضافة منتج جديد");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        
        ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = createProductForm(null);
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createProductFromForm(grid, null);
            }
            return null;
        });
        
        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(product -> {
            loadProducts();
            showSuccess("تم بنجاح", "تم إضافة المنتج " + product.getName());
        });
    }

    private void showSellDialog(Product product) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("بيع منتج");
        dialog.setHeaderText("بيع " + product.getName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        
        ButtonType sellButtonType = new ButtonType("بيع", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sellButtonType, ButtonType.CANCEL);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label stockLabel = new Label("المتوفر: " + product.getQuantity());
        Label priceLabel = new Label("سعر البيع: " + product.getSalePrice() + " ج.م");
        
        Spinner<Integer> quantitySpinner = new Spinner<>(1, product.getQuantity(), 1);
        quantitySpinner.setEditable(true);
        
        Label totalLabel = new Label("الإجمالي: " + product.getSalePrice() + " ج.م");
        totalLabel.getStyleClass().add("total-label");
        
        quantitySpinner.valueProperty().addListener((obs, old, newVal) -> {
            BigDecimal total = product.getSalePrice().multiply(BigDecimal.valueOf(newVal));
            totalLabel.setText("الإجمالي: " + total + " ج.م");
        });
        
        content.getChildren().addAll(
            stockLabel, priceLabel,
            new Label("الكمية:"), quantitySpinner,
            totalLabel
        );
        
        dialog.getDialogPane().setContent(content);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sellButtonType) {
                return quantitySpinner.getValue();
            }
            return null;
        });
        
        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(quantity -> {
            try {
                productService.sellProduct(product.getId(), quantity);
                loadProducts();
                showSuccess("تم البيع", "تم بيع " + quantity + " × " + product.getName());
            } catch (Exception e) {
                showError("خطأ", e.getMessage());
            }
        });
    }

    private void showAddStockDialog(Product product) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("إضافة مخزون");
        dialog.setHeaderText("إضافة مخزون لـ " + product.getName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        
        ButtonType addButtonType = new ButtonType("إضافة", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 1000, 1);
        quantitySpinner.setEditable(true);
        
        TextField costField = new TextField();
        costField.setPromptText("تكلفة الشراء (اختياري)");
        
        grid.add(new Label("الكمية:"), 0, 0);
        grid.add(quantitySpinner, 1, 0);
        grid.add(new Label("التكلفة:"), 0, 1);
        grid.add(costField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    BigDecimal cost = costField.getText().isEmpty() ? 
                        null : new BigDecimal(costField.getText());
                    productService.addStock(product.getId(), quantitySpinner.getValue(), cost);
                    loadProducts();
                    showSuccess("تم بنجاح", "تم إضافة " + quantitySpinner.getValue() + " وحدة");
                } catch (NumberFormatException e) {
                    showError("خطأ", "يرجى إدخال تكلفة صحيحة");
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    private void editProduct(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("تعديل منتج");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        
        ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = createProductForm(product);
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createProductFromForm(grid, product);
            }
            return null;
        });
        
        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(p -> {
            loadProducts();
            showSuccess("تم بنجاح", "تم تحديث المنتج");
        });
    }

    private GridPane createProductForm(Product product) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField(product != null ? product.getName() : "");
        nameField.setId("nameField");
        nameField.setPromptText("اسم المنتج");
        
        ComboBox<Product.ProductCategory> categoryCombo = new ComboBox<>();
        categoryCombo.setId("categoryCombo");
        categoryCombo.setItems(FXCollections.observableArrayList(Product.ProductCategory.values()));
        categoryCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Product.ProductCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getArabicName());
            }
        });
        categoryCombo.setButtonCell(categoryCombo.getCellFactory().call(null));
        if (product != null) categoryCombo.setValue(product.getCategory());
        
        TextField purchasePriceField = new TextField(product != null ? product.getPurchasePrice().toString() : "");
        purchasePriceField.setId("purchasePriceField");
        purchasePriceField.setPromptText("سعر الشراء");
        
        TextField salePriceField = new TextField(product != null ? product.getSalePrice().toString() : "");
        salePriceField.setId("salePriceField");
        salePriceField.setPromptText("سعر البيع");
        
        Spinner<Integer> quantitySpinner = new Spinner<>(0, 10000, product != null ? product.getQuantity() : 0);
        quantitySpinner.setId("quantitySpinner");
        quantitySpinner.setEditable(true);
        
        Spinner<Integer> thresholdSpinner = new Spinner<>(1, 100, product != null ? product.getLowStockThreshold() : 5);
        thresholdSpinner.setId("thresholdSpinner");
        thresholdSpinner.setEditable(true);
        
        grid.add(new Label("الاسم:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("التصنيف:"), 0, 1);
        grid.add(categoryCombo, 1, 1);
        grid.add(new Label("سعر الشراء:"), 0, 2);
        grid.add(purchasePriceField, 1, 2);
        grid.add(new Label("سعر البيع:"), 0, 3);
        grid.add(salePriceField, 1, 3);
        
        if (product == null) {
            grid.add(new Label("الكمية الأولية:"), 0, 4);
            grid.add(quantitySpinner, 1, 4);
        }
        
        grid.add(new Label("حد التنبيه:"), 0, 5);
        grid.add(thresholdSpinner, 1, 5);
        
        return grid;
    }

    @SuppressWarnings("unchecked")
    private Product createProductFromForm(GridPane grid, Product existingProduct) {
        TextField nameField = (TextField) grid.lookup("#nameField");
        ComboBox<Product.ProductCategory> categoryCombo = (ComboBox<Product.ProductCategory>) grid.lookup("#categoryCombo");
        TextField purchasePriceField = (TextField) grid.lookup("#purchasePriceField");
        TextField salePriceField = (TextField) grid.lookup("#salePriceField");
        Spinner<Integer> quantitySpinner = (Spinner<Integer>) grid.lookup("#quantitySpinner");
        Spinner<Integer> thresholdSpinner = (Spinner<Integer>) grid.lookup("#thresholdSpinner");
        
        if (nameField.getText().isEmpty() || categoryCombo.getValue() == null ||
            purchasePriceField.getText().isEmpty() || salePriceField.getText().isEmpty()) {
            showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
            return null;
        }
        
        try {
            if (existingProduct != null) {
                existingProduct.setName(nameField.getText());
                existingProduct.setCategory(categoryCombo.getValue());
                existingProduct.setPurchasePrice(new BigDecimal(purchasePriceField.getText()));
                existingProduct.setSalePrice(new BigDecimal(salePriceField.getText()));
                existingProduct.setLowStockThreshold(thresholdSpinner.getValue());
                return productService.updateProduct(existingProduct.getId(), existingProduct);
            } else {
                Product product = new Product();
                product.setName(nameField.getText());
                product.setCategory(categoryCombo.getValue());
                product.setPurchasePrice(new BigDecimal(purchasePriceField.getText()));
                product.setSalePrice(new BigDecimal(salePriceField.getText()));
                product.setQuantity(quantitySpinner != null ? quantitySpinner.getValue() : 0);
                product.setLowStockThreshold(thresholdSpinner.getValue());
                return productService.createProduct(product);
            }
        } catch (NumberFormatException e) {
            showError("خطأ", "يرجى إدخال أسعار صحيحة");
            return null;
        }
    }

    private void showProductHistory(Product product) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("سجل حركات المنتج");
        dialog.setHeaderText(product.getName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        List<InventoryLog> logs = productService.getProductHistory(product.getId());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setMinWidth(500);
        
        if (logs.isEmpty()) {
            content.getChildren().add(new Label("لا توجد حركات مسجلة"));
        } else {
            for (InventoryLog log : logs) {
                HBox row = new HBox(15);
                row.getStyleClass().add("history-row");
                
                Label dateLabel = new Label(log.getCreatedAt().toLocalDate().toString());
                Label typeLabel = new Label(log.getType().getArabicName());
                Label changeLabel = new Label((log.getQuantityChange() >= 0 ? "+" : "") + log.getQuantityChange());
                changeLabel.setStyle(log.getQuantityChange() >= 0 ? "-fx-text-fill: #22c55e;" : "-fx-text-fill: #ef4444;");
                Label reasonLabel = new Label(log.getReason() != null ? log.getReason() : "-");
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                
                row.getChildren().addAll(dateLabel, typeLabel, changeLabel, spacer, reasonLabel);
                content.getChildren().add(row);
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(400);
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }

    private void deleteProduct(Product product) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText("هل أنت متأكد من حذف " + product.getName() + "؟");
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            productService.deactivateProduct(product.getId());
            loadProducts();
            showSuccess("تم بنجاح", "تم حذف المنتج");
        }
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
