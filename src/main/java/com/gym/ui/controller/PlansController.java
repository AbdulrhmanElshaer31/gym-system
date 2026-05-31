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
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

@Component
public class PlansController {

    @FXML private TableView<Plan> plansTable;
    @FXML private TableColumn<Plan, Long> colId;
    @FXML private TableColumn<Plan, String> colName;
    @FXML private TableColumn<Plan, String> colDuration;
    @FXML private TableColumn<Plan, String> colPrice;
    @FXML private TableColumn<Plan, String> colRenewalPrice;
    @FXML private TableColumn<Plan, String> colStatus;
    @FXML private TableColumn<Plan, String> colMembers;
    @FXML private TableColumn<Plan, Void> colActions;

    @Autowired
    private PlanService planService;

    private final ObservableList<Plan> plansList = FXCollections.observableArrayList();
    private Map<Long, Long> membersCount;

    @FXML
    public void initialize() {
        setupTable();
        loadPlans();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        // ✅ عدّل: عرض الفترة والحصص معاً
        colDuration.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCombinedText()));

        colPrice.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPrice() + " ج.م"));

        colRenewalPrice.setCellValueFactory(cellData -> {
            BigDecimal renewalPrice = cellData.getValue().getRenewalPrice();
            if (renewalPrice != null) {
                return new SimpleStringProperty(renewalPrice + " ج.م");
            } else {
                return new SimpleStringProperty("نفس السعر");
            }
        });

        colRenewalPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("نفس السعر")) {
                        setStyle("-fx-text-fill: #5F9598; -fx-font-style: italic;");
                    } else {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getActive() ? "مفعّلة" : "معطّلة"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("مفعّلة")) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colMembers.setCellValueFactory(cellData -> {
            Long count = membersCount != null ? membersCount.getOrDefault(cellData.getValue().getId(), 0L) : 0L;
            return new SimpleStringProperty(count + " مشترك");
        });

        setupActionsColumn();

        plansTable.setItems(plansList);

        // Default sort by plan number (id) ascending
        colId.setSortType(TableColumn.SortType.ASCENDING);
        plansTable.getSortOrder().setAll(colId);
    }

    private void setupActionsColumn() {
        Callback<TableColumn<Plan, Void>, TableCell<Plan, Void>> cellFactory = param -> new TableCell<>() {
            private final MenuButton menuButton = new MenuButton("⋮");

            {
                menuButton.getStyleClass().add("action-menu");

                MenuItem editItem = new MenuItem("تعديل");
                editItem.setOnAction(e -> editPlan(getTableView().getItems().get(getIndex())));

                MenuItem toggleItem = new MenuItem("تغيير الحالة");
                toggleItem.setOnAction(e -> togglePlanStatus(getTableView().getItems().get(getIndex())));

                MenuItem deleteItem = new MenuItem("حذف");
                deleteItem.setOnAction(e -> deletePlan(getTableView().getItems().get(getIndex())));
                deleteItem.getStyleClass().add("danger-menu-item");

                menuButton.getItems().addAll(editItem, toggleItem, new SeparatorMenuItem(), deleteItem);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : menuButton);
            }
        };

        colActions.setCellFactory(cellFactory);
    }

    private void loadPlans() {
        membersCount = planService.getMembersCountByPlan();
        plansList.clear();
        var plans = planService.getAllPlans();
        plans.sort(Comparator.comparing(Plan::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        plansList.addAll(plans);
        plansTable.sort();
    }

    @FXML
    private void showAddPlanDialog() {
        Dialog<Plan> dialog = new Dialog<>();
        dialog.setTitle("إضافة خطة جديدة");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = createPlanForm(null);
        dialog.getDialogPane().setContent(grid);

        TextField nameField = (TextField) grid.lookup("#nameField");
        TextField durationField = (TextField) grid.lookup("#durationField");
        TextField sessionsField = (TextField) grid.lookup("#sessionsField");  // ✅ جديد
        TextField priceField = (TextField) grid.lookup("#priceField");
        TextField renewalPriceField = (TextField) grid.lookup("#renewalPriceField");
        TextArea descField = (TextArea) grid.lookup("#descField");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().isEmpty() || durationField.getText().isEmpty() || priceField.getText().isEmpty()) {
                    showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
                    return null;
                }

                try {
                    Plan plan = new Plan();
                    plan.setName(nameField.getText());
                    plan.setDurationDays(Integer.parseInt(durationField.getText()));

                    // ✅ إضافة عدد الحصص (إذا كانت فارغة تبقى null)
                    if (!sessionsField.getText().isEmpty()) {
                        plan.setNumberOfSessions(Integer.parseInt(sessionsField.getText()));
                    }

                    plan.setPrice(new BigDecimal(priceField.getText()));

                    if (!renewalPriceField.getText().isEmpty()) {
                        plan.setRenewalPrice(new BigDecimal(renewalPriceField.getText()));
                    }

                    plan.setDescription(descField.getText());

                    return planService.createPlan(plan);
                } catch (NumberFormatException e) {
                    showError("خطأ", "يرجى إدخال أرقام صحيحة");
                    return null;
                }
            }
            return null;
        });

        Optional<Plan> result = dialog.showAndWait();
        result.ifPresent(plan -> {
            loadPlans();
            showSuccess("تم بنجاح", "تم إضافة الخطة " + plan.getName());
        });
    }

    private void editPlan(Plan plan) {
        Dialog<Plan> dialog = new Dialog<>();
        dialog.setTitle("تعديل الخطة");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = createPlanForm(plan);
        dialog.getDialogPane().setContent(grid);

        TextField nameField = (TextField) grid.lookup("#nameField");
        TextField durationField = (TextField) grid.lookup("#durationField");
        TextField sessionsField = (TextField) grid.lookup("#sessionsField");  // ✅ جديد
        TextField priceField = (TextField) grid.lookup("#priceField");
        TextField renewalPriceField = (TextField) grid.lookup("#renewalPriceField");
        TextArea descField = (TextArea) grid.lookup("#descField");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().isEmpty() || durationField.getText().isEmpty() || priceField.getText().isEmpty()) {
                    showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
                    return null;
                }

                try {
                    plan.setName(nameField.getText());
                    plan.setDurationDays(Integer.parseInt(durationField.getText()));

                    // ✅ تحديث عدد الحصص
                    if (!sessionsField.getText().isEmpty()) {
                        plan.setNumberOfSessions(Integer.parseInt(sessionsField.getText()));
                    } else {
                        plan.setNumberOfSessions(null);
                    }

                    plan.setPrice(new BigDecimal(priceField.getText()));

                    if (!renewalPriceField.getText().isEmpty()) {
                        plan.setRenewalPrice(new BigDecimal(renewalPriceField.getText()));
                    } else {
                        plan.setRenewalPrice(null);
                    }

                    plan.setDescription(descField.getText());

                    return planService.updatePlan(plan.getId(), plan);
                } catch (NumberFormatException e) {
                    showError("خطأ", "يرجى إدخال أرقام صحيحة");
                    return null;
                }
            }
            return null;
        });

        Optional<Plan> result = dialog.showAndWait();
        result.ifPresent(p -> {
            loadPlans();
            showSuccess("تم بنجاح", "تم تحديث الخطة");
        });
    }

    private GridPane createPlanForm(Plan plan) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(plan != null ? plan.getName() : "");
        nameField.setId("nameField");
        nameField.setPromptText("اسم الخطة");

        TextField durationField = new TextField(plan != null ? String.valueOf(plan.getDurationDays()) : "");
        durationField.setId("durationField");
        durationField.setPromptText("عدد الأيام");

        // ✅ حقل جديد: عدد الحصص
        TextField sessionsField = new TextField(
                plan != null && plan.getNumberOfSessions() != null ? String.valueOf(plan.getNumberOfSessions()) : ""
        );
        sessionsField.setId("sessionsField");
        sessionsField.setPromptText("عدد الحصص (اختياري)");

        TextField priceField = new TextField(plan != null ? plan.getPrice().toString() : "");
        priceField.setId("priceField");
        priceField.setPromptText("السعر");

        TextField renewalPriceField = new TextField(
                plan != null && plan.getRenewalPrice() != null ? plan.getRenewalPrice().toString() : ""
        );
        renewalPriceField.setId("renewalPriceField");
        renewalPriceField.setPromptText("سعر التجديد (اختياري)");

        TextArea descField = new TextArea(plan != null && plan.getDescription() != null ? plan.getDescription() : "");
        descField.setId("descField");
        descField.setPromptText("الوصف");
        descField.setWrapText(true);
        descField.setPrefRowCount(3);

        // أزرار سريعة للمدة
        HBox durationButtons = new HBox(5);
        Button btn30 = new Button("شهر");
        btn30.setOnAction(e -> durationField.setText("30"));
        Button btn90 = new Button("3 أشهر");
        btn90.setOnAction(e -> durationField.setText("90"));
        Button btn180 = new Button("6 أشهر");
        btn180.setOnAction(e -> durationField.setText("180"));
        Button btn365 = new Button("سنة");
        btn365.setOnAction(e -> durationField.setText("365"));
        durationButtons.getChildren().addAll(btn30, btn90, btn180, btn365);

        // ✅ أزرار سريعة لعدد الحصص
        HBox sessionsButtons = new HBox(5);
        for (int count : new int[]{4, 8, 12, 16, 20}) {
            Button btn = new Button(count + " حصة");
            btn.setStyle("-fx-font-size: 10; -fx-padding: 5;");
            btn.setOnAction(e -> sessionsField.setText(String.valueOf(count)));
            sessionsButtons.getChildren().add(btn);
        }

        Label renewalHint = new Label("💡 إذا تركته فارغاً سيستخدم السعر الأساسي للتجديد");
        renewalHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #5F9598;");

        // ✅ ترتيب الحقول
        grid.add(new Label("اسم الخطة:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("الفترة الزمنية (بالأيام):"), 0, 1);
        grid.add(durationField, 1, 1);
        grid.add(new Label(""), 0, 2);
        grid.add(durationButtons, 1, 2);

        grid.add(new Label("عدد الحصص:"), 0, 3);
        grid.add(sessionsField, 1, 3);
        grid.add(new Label(""), 0, 4);
        grid.add(sessionsButtons, 1, 4);

        grid.add(new Label("السعر (ج.م):"), 0, 5);
        grid.add(priceField, 1, 5);
        grid.add(new Label("سعر التجديد (ج.م):"), 0, 6);
        grid.add(renewalPriceField, 1, 6);
        grid.add(new Label(""), 0, 7);
        grid.add(renewalHint, 1, 7);
        grid.add(new Label("الوصف:"), 0, 8);
        grid.add(descField, 1, 8);

        return grid;
    }

    private void togglePlanStatus(Plan plan) {
        planService.togglePlanStatus(plan.getId());
        loadPlans();
        showSuccess("تم بنجاح", "تم تغيير حالة الخطة");
    }

    private void deletePlan(Plan plan) {
        if (!planService.canDeletePlan(plan.getId())) {
            showError("لا يمكن الحذف", "هذه الخطة مستخدمة من قبل مشتركين.\nيمكنك تعطيلها بدلاً من حذفها.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText("هل أنت متأكد من حذف خطة " + plan.getName() + "؟");
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            planService.deletePlan(plan.getId());
            loadPlans();
            showSuccess("تم بنجاح", "تم حذف الخطة");
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