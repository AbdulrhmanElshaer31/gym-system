package com.gym.ui.controller;

import com.gym.entity.Coach;
import com.gym.service.CoachService;
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
public class CoachesController {

    @FXML private TableView<Coach> coachesTable;
    @FXML private TableColumn<Coach, Long> colId;
    @FXML private TableColumn<Coach, String> colName;
    @FXML private TableColumn<Coach, String> colSpecialty;
    @FXML private TableColumn<Coach, String> colSalary;
    @FXML private TableColumn<Coach, String> colMembers;
    @FXML private TableColumn<Coach, String> colStatus;
    @FXML private TableColumn<Coach, String> colNotes;
    @FXML private TableColumn<Coach, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatus;
    @FXML private Label lblTotalCoaches;

    @Autowired
    private CoachService coachService;

    private final ObservableList<Coach> coachesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        try {
            setupTable();
            setupFilters();
            loadCoaches();
        } catch (Exception e) {
            showError("خطأ في التهيئة", e.getMessage());
        }
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSpecialty.setCellValueFactory(new PropertyValueFactory<>("specialty"));

        colSalary.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%,.0f ج.م", cellData.getValue().getSalary())));
        colSalary.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #22c55e;");
                }
            }
        });

        colMembers.setCellValueFactory(cellData -> {
            Long count = coachService.getMemberCountForCoach(cellData.getValue().getId());
            return new SimpleStringProperty(count + " متدرب");
        });

        colStatus.setCellValueFactory(cellData -> {
            boolean active = cellData.getValue().getActive();
            return new SimpleStringProperty(active ? "نشط" : "معطل");
        });
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("نشط".equals(item)) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colNotes.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getNotes() != null ? cellData.getValue().getNotes() : "-"));

        setupActionsColumn();
        coachesTable.setItems(coachesList);
    }

    private void setupActionsColumn() {
        Callback<TableColumn<Coach, Void>, TableCell<Coach, Void>> cellFactory = param -> new TableCell<>() {
            private final MenuButton menuButton = new MenuButton("⋮");

            {
                MenuItem viewItem = new MenuItem("عرض التفاصيل");
                viewItem.setOnAction(e -> showCoachDetails(getTableView().getItems().get(getIndex())));

                MenuItem editItem = new MenuItem("تعديل");
                editItem.setOnAction(e -> editCoach(getTableView().getItems().get(getIndex())));

                MenuItem toggleItem = new MenuItem("تفعيل/تعطيل");
                toggleItem.setOnAction(e -> toggleCoachStatus(getTableView().getItems().get(getIndex())));

                MenuItem deleteItem = new MenuItem("حذف");
                deleteItem.setOnAction(e -> deleteCoach(getTableView().getItems().get(getIndex())));

                menuButton.getItems().addAll(viewItem, editItem, new SeparatorMenuItem(), toggleItem, new SeparatorMenuItem(), deleteItem);
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
        filterStatus.setItems(FXCollections.observableArrayList("الكل", "نشط", "معطل"));
        filterStatus.setValue("الكل");
        filterStatus.setOnAction(e -> applyFilters());
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void loadCoaches() {
        try {
            coachesList.clear();
            List<Coach> coaches = coachService.getAllActiveCoaches();
            if (coaches != null) {
                coachesList.addAll(coaches);
            }
            updateCoachCount();
        } catch (Exception e) {
            showError("خطأ في تحميل المدربين", e.getMessage());
        }
    }

    private void applyFilters() {
        try {
            List<Coach> filtered = coachService.getAllActiveCoaches();
            String search = searchField.getText().trim().toLowerCase();
            if (!search.isEmpty()) {
                filtered = filtered.stream()
                        .filter(c -> c.getName().toLowerCase().contains(search) ||
                                c.getSpecialty().toLowerCase().contains(search))
                        .toList();
            }

            String statusFilter = filterStatus.getValue();
            if (statusFilter != null && !statusFilter.equals("الكل")) {
                final String status = statusFilter;
                filtered = filtered.stream()
                        .filter(c -> (status.equals("نشط") && c.getActive()) ||
                                (status.equals("معطل") && !c.getActive()))
                        .toList();
            }

            coachesList.clear();
            coachesList.addAll(filtered);
            updateCoachCount();
        } catch (Exception e) {
            showError("خطأ في التصفية", e.getMessage());
        }
    }

    private void updateCoachCount() {
        lblTotalCoaches.setText("إجمالي المدربين: " + coachesList.size());
    }

    @FXML
    private void showAddCoachDialog() {
        try {
            Dialog<Coach> dialog = new Dialog<>();
            dialog.setTitle("إضافة مدرب جديد");

            // ✅ أضف CSS styling للـ dialog
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

            ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(15);
            grid.setPadding(new Insets(25));

            TextField nameField = new TextField();
            nameField.setPromptText("اسم المدرب");

            TextField specialtyField = new TextField();
            specialtyField.setPromptText("التخصص");

            TextField salaryField = new TextField();
            salaryField.setPromptText("الراتب الشهري");

            TextArea notesField = new TextArea();
            notesField.setPromptText("ملاحظات (اختياري)");
            notesField.setPrefRowCount(3);

            grid.add(new Label("الاسم:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("التخصص:"), 0, 1);
            grid.add(specialtyField, 1, 1);
            grid.add(new Label("الراتب:"), 0, 2);
            grid.add(salaryField, 1, 2);
            grid.add(new Label("ملاحظات:"), 0, 3);
            grid.add(notesField, 1, 3);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    if (nameField.getText().isEmpty() || specialtyField.getText().isEmpty() || salaryField.getText().isEmpty()) {
                        showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
                        return null;
                    }

                    try {
                        Coach coach = new Coach();
                        coach.setName(nameField.getText().trim());
                        coach.setSpecialty(specialtyField.getText().trim());
                        coach.setSalary(new BigDecimal(salaryField.getText().trim()));
                        coach.setNotes(notesField.getText().isEmpty() ? null : notesField.getText().trim());
                        coach.setActive(true);
                        coach.setDeleted(false);
                        return coachService.createCoach(coach);
                    } catch (NumberFormatException e) {
                        showError("خطأ", "يرجى إدخال راتب صحيح (أرقام فقط)");
                        return null;
                    }
                }
                return null;
            });

            Optional<Coach> result = dialog.showAndWait();
            result.ifPresent(coach -> {
                loadCoaches();
                showSuccess("تم بنجاح", "تم إضافة المدرب " + coach.getName());
            });
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
        }
    }

    private void showCoachDetails(Coach coach) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("تفاصيل المدرب");
            alert.setHeaderText(coach.getName());

            // ✅ أضف CSS styling للـ alert
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

            Long memberCount = coachService.getMemberCountForCoach(coach.getId());

            alert.setContentText(
                    "التخصص: " + coach.getSpecialty() +
                            "\nالراتب: " + String.format("%,.0f ج.م", coach.getSalary()) +
                            "\nالحالة: " + (coach.getActive() ? "نشط" : "معطل") +
                            "\nعدد المتدربين: " + memberCount +
                            "\nملاحظات: " + (coach.getNotes() != null ? coach.getNotes() : "-")
            );
            alert.showAndWait();
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
        }
    }

    private void editCoach(Coach coach) {
        try {
            Dialog<Coach> dialog = new Dialog<>();
            dialog.setTitle("تعديل بيانات المدرب");

            // ✅ أضف CSS styling للـ dialog
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

            ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            TextField nameField = new TextField(coach.getName());
            TextField specialtyField = new TextField(coach.getSpecialty());
            TextField salaryField = new TextField(coach.getSalary().toString());
            TextArea notesField = new TextArea(coach.getNotes() != null ? coach.getNotes() : "");
            notesField.setPrefRowCount(3);
            CheckBox activeCheckBox = new CheckBox("نشط");
            activeCheckBox.setSelected(coach.getActive());

            grid.add(new Label("الاسم:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("التخصص:"), 0, 1);
            grid.add(specialtyField, 1, 1);
            grid.add(new Label("الراتب:"), 0, 2);
            grid.add(salaryField, 1, 2);
            grid.add(new Label("ملاحظات:"), 0, 3);
            grid.add(notesField, 1, 3);
            grid.add(activeCheckBox, 1, 4);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    if (nameField.getText().isEmpty() || specialtyField.getText().isEmpty()) {
                        showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
                        return null;
                    }

                    try {
                        coach.setName(nameField.getText().trim());
                        coach.setSpecialty(specialtyField.getText().trim());
                        coach.setSalary(new BigDecimal(salaryField.getText().trim()));
                        coach.setNotes(notesField.getText().isEmpty() ? null : notesField.getText().trim());
                        coach.setActive(activeCheckBox.isSelected());
                        return coachService.updateCoach(coach.getId(), coach);
                    } catch (NumberFormatException e) {
                        showError("خطأ", "يرجى إدخال راتب صحيح");
                        return null;
                    }
                }
                return null;
            });

            Optional<Coach> result = dialog.showAndWait();
            result.ifPresent(c -> {
                loadCoaches();
                showSuccess("تم بنجاح", "تم تحديث بيانات المدرب");
            });
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
        }
    }

    private void toggleCoachStatus(Coach coach) {
        try {
            coachService.toggleCoachStatus(coach.getId());
            loadCoaches();
            String status = coach.getActive() ? "معطل" : "نشط";
            showSuccess("تم بنجاح", "تم تغيير حالة المدرب إلى: " + status);
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
        }
    }

    private void deleteCoach(Coach coach) {
        try {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("تأكيد الحذف");
            confirm.setHeaderText("هل أنت متأكد من حذف " + coach.getName() + "؟");
            confirm.setContentText("سيتم إخفاء المدرب ولن يظهر في القائمة");

            // ✅ أضف CSS styling للـ alert
            confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                coachService.softDeleteCoach(coach.getId());
                loadCoaches();
                showSuccess("تم بنجاح", "تم حذف المدرب");
            }
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
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