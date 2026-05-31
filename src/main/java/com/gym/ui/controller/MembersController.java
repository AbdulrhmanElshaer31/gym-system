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

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class MembersController {

    @FXML private TableView<Member> membersTable;
    @FXML private TableColumn<Member, Long> colId;
    @FXML private TableColumn<Member, String> colMemberId;
    @FXML private TableColumn<Member, String> colName;
    @FXML private TableColumn<Member, String> colGender;
    @FXML private TableColumn<Member, String> colPhone;
    @FXML private TableColumn<Member, String> colCoach;
    @FXML private TableColumn<Member, String> colPlan;
    @FXML private TableColumn<Member, String> colEndDate;
    @FXML private TableColumn<Member, String> colStatus;
    @FXML private TableColumn<Member, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterGender;
    @FXML private Label lblTotalMembers;

    @Autowired
    private MemberService memberService;

    @Autowired
    private PlanService planService;

    @Autowired
    private CoachService coachService;

    private final ObservableList<Member> membersList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        try {
            setupTable();
            setupFilters();
            loadMembers();
        } catch (Exception e) {
            showError("خطأ في التهيئة", e.getMessage());
        }
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMemberId.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colGender.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getGender() != null ?
                        cellData.getValue().getGender().getArabicName() : "-"));

        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        colCoach.setCellValueFactory(cellData -> {
            Coach coach = cellData.getValue().getCoach();
            return new SimpleStringProperty(coach != null ? coach.getName() : "-");
        });

        colPlan.setCellValueFactory(cellData -> {
            Plan plan = cellData.getValue().getCurrentPlan();
            return new SimpleStringProperty(plan != null ? plan.getName() : "-");
        });

        colEndDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSubscriptionEndDate() != null ?
                        cellData.getValue().getSubscriptionEndDate().format(dateFormatter) : "-"));

        colStatus.setCellValueFactory(cellData -> {
            Member member = cellData.getValue();
            LocalDate endDate = member.getSubscriptionEndDate();
            LocalDate today = LocalDate.now();
            String status;

            if (endDate == null) {
                status = "-";
            } else if (endDate.isBefore(today)) {
                status = "منتهي";
            } else if (endDate.minusDays(7).isBefore(today)) {
                status = "قارب على الانتهاء";
            } else {
                status = "نشط";
            }

            return new SimpleStringProperty(status);
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
                    switch (item) {
                        case "نشط" -> setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                        case "قارب على الانتهاء" -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                        case "منتهي" -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        default -> setStyle("");
                    }
                }
            }
        });

        setupActionsColumn();
        membersTable.setItems(membersList);
    }

    private void setupActionsColumn() {
        Callback<TableColumn<Member, Void>, TableCell<Member, Void>> cellFactory = param -> new TableCell<>() {
            private final MenuButton menuButton = new MenuButton("⋮");

            {
                MenuItem view = new MenuItem("عرض التفاصيل");
                view.setOnAction(e -> showMemberDetails(getTableView().getItems().get(getIndex())));

                MenuItem edit = new MenuItem("تعديل");
                edit.setOnAction(e -> editMember(getTableView().getItems().get(getIndex())));

                MenuItem delete = new MenuItem("حذف");
                delete.setOnAction(e -> deleteMember(getTableView().getItems().get(getIndex())));

                menuButton.getItems().addAll(view, edit, new SeparatorMenuItem(), delete);
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
        filterStatus.setItems(FXCollections.observableArrayList("الكل", "نشط", "قارب على الانتهاء", "منتهي"));
        filterStatus.setValue("الكل");

        filterGender.setItems(FXCollections.observableArrayList("الكل", "ذكر", "أنثى"));
        filterGender.setValue("الكل");

        searchField.textProperty().addListener((a, b, c) -> applyFilters());
        filterStatus.setOnAction(e -> applyFilters());
        filterGender.setOnAction(e -> applyFilters());
    }

    private void loadMembers() {
        try {
            List<Member> members = memberService.getAllActiveMembers();
            membersList.clear();
            if (members != null) {
                membersList.addAll(members);
            }
            lblTotalMembers.setText("إجمالي المشتركين: " + membersList.size());
        } catch (Exception e) {
            showError("خطأ في تحميل المشتركين", e.getMessage());
        }
    }

    private void applyFilters() {
        try {
            List<Member> filtered = memberService.getAllActiveMembers();

            String search = searchField.getText().trim().toLowerCase();
            if (!search.isEmpty()) {
                filtered = filtered.stream()
                        .filter(m -> m.getName().toLowerCase().contains(search) ||
                                m.getPhone().toLowerCase().contains(search) ||
                                (m.getMemberId() != null && m.getMemberId().toLowerCase().contains(search)))
                        .toList();
            }

            String statusFilter = filterStatus.getValue();
            if (statusFilter != null && !statusFilter.equals("الكل")) {
                final String status = statusFilter;
                filtered = filtered.stream()
                        .filter(m -> {
                            LocalDate endDate = m.getSubscriptionEndDate();
                            LocalDate today = LocalDate.now();
                            String memberStatus;

                            if (endDate == null) {
                                memberStatus = "-";
                            } else if (endDate.isBefore(today)) {
                                memberStatus = "منتهي";
                            } else if (endDate.minusDays(7).isBefore(today)) {
                                memberStatus = "قارب على الانتهاء";
                            } else {
                                memberStatus = "نشط";
                            }
                            return memberStatus.equals(status);
                        })
                        .toList();
            }

            String genderFilter = filterGender.getValue();
            if (genderFilter != null && !genderFilter.equals("الكل")) {
                String genderValue = genderFilter.equals("ذكر") ? "MALE" : "FEMALE";
                filtered = filtered.stream()
                        .filter(m -> m.getGender() != null && m.getGender().name().equals(genderValue))
                        .toList();
            }

            membersList.clear();
            membersList.addAll(filtered);
        } catch (Exception e) {
            showError("خطأ في التصفية", e.getMessage());
        }
    }

    @FXML
    private void showAddMemberDialog() {
        try {
            Dialog<Member> dialog = new Dialog<>();
            dialog.setTitle("إضافة مشترك جديد");

            // ✅ أضف CSS styling للـ dialog
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

            ButtonType saveBtn = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(15);
            grid.setPadding(new Insets(20));

            TextField nameField = new TextField();
            nameField.setPromptText("اسم المشترك");

            TextField phoneField = new TextField();
            phoneField.setPromptText("رقم الهاتف");

            ComboBox<Member.Gender> genderCombo = new ComboBox<>();
            genderCombo.setItems(FXCollections.observableArrayList(Member.Gender.values()));
            genderCombo.setPromptText("اختر الجنس");

            genderCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Member.Gender item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getArabicName());
                }
            });
            genderCombo.setButtonCell(genderCombo.getCellFactory().call(null));

            ComboBox<Coach> coachCombo = new ComboBox<>();
            coachCombo.setItems(FXCollections.observableArrayList(coachService.getAllWorkingCoaches()));
            coachCombo.setPromptText("اختر الكوتش (اختياري)");

            coachCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Coach item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName() + " - " + item.getSpecialty());
                }
            });
            coachCombo.setButtonCell(coachCombo.getCellFactory().call(null));

            ComboBox<Plan> planCombo = new ComboBox<>();
            planCombo.setItems(FXCollections.observableArrayList(planService.getActivePlans()));
            planCombo.setPromptText("اختر الخطة");
            planCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Plan item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            planCombo.setButtonCell(planCombo.getCellFactory().call(null));

            grid.addRow(0, new Label("الاسم"), nameField);
            grid.addRow(1, new Label("الجنس"), genderCombo);
            grid.addRow(2, new Label("الهاتف"), phoneField);
            grid.addRow(3, new Label("الكوتش"), coachCombo);
            grid.addRow(4, new Label("الخطة"), planCombo);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(btn -> {
                if (btn == saveBtn) {
                    if (nameField.getText().isEmpty() || phoneField.getText().isEmpty() ||
                            genderCombo.getValue() == null || planCombo.getValue() == null) {
                        showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
                        return null;
                    }

                    Member member = new Member();
                    member.setName(nameField.getText().trim());
                    member.setPhone(phoneField.getText().trim());
                    member.setGender(genderCombo.getValue());
                    member.setCoach(coachCombo.getValue());
                    member.setCurrentPlan(planCombo.getValue());

                    try {
                        // ✅ FIXED: Pass both member and plan to createMember
                        memberService.createMember(member, planCombo.getValue());
                        return member;
                    } catch (Exception e) {
                        showError("خطأ", "فشل حفظ المشترك: " + e.getMessage());
                        return null;
                    }
                }
                return null;
            });

            Optional<Member> result = dialog.showAndWait();
            result.ifPresent(m -> {
                loadMembers();
                showSuccess("تم بنجاح", "تم إضافة المشترك " + m.getName());
            });
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
        }
    }

    private void editMember(Member member) {
        try {
            Dialog<Member> dialog = new Dialog<>();
            dialog.setTitle("تعديل المشترك");

            // ✅ أضف CSS styling للـ dialog
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

            ButtonType saveBtn = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            TextField nameField = new TextField(member.getName());
            TextField phoneField = new TextField(member.getPhone());

            ComboBox<Member.Gender> genderCombo = new ComboBox<>();
            genderCombo.setItems(FXCollections.observableArrayList(Member.Gender.values()));
            genderCombo.setValue(member.getGender());
            genderCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Member.Gender item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getArabicName());
                }
            });
            genderCombo.setButtonCell(genderCombo.getCellFactory().call(null));

            ComboBox<Coach> coachCombo = new ComboBox<>();
            coachCombo.setItems(FXCollections.observableArrayList(coachService.getAllWorkingCoaches()));
            coachCombo.setValue(member.getCoach());
            coachCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Coach item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName() + " - " + item.getSpecialty());
                }
            });
            coachCombo.setButtonCell(coachCombo.getCellFactory().call(null));

            ComboBox<Plan> planCombo = new ComboBox<>();
            planCombo.setItems(FXCollections.observableArrayList(planService.getActivePlans()));
            planCombo.setValue(member.getCurrentPlan());
            planCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Plan item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            planCombo.setButtonCell(planCombo.getCellFactory().call(null));

            grid.addRow(0, new Label("الاسم"), nameField);
            grid.addRow(1, new Label("الهاتف"), phoneField);
            grid.addRow(2, new Label("الكوتش"), coachCombo);
            grid.addRow(3, new Label("الخطة"), planCombo);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(btn -> {
                if (btn == saveBtn) {
                    if (nameField.getText().isEmpty() || phoneField.getText().isEmpty() ||
                            genderCombo.getValue() == null || planCombo.getValue() == null) {
                        showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
                        return null;
                    }

                    try {
                        member.setName(nameField.getText().trim());
                        member.setPhone(phoneField.getText().trim());
                        member.setGender(genderCombo.getValue());
                        member.setCoach(coachCombo.getValue());
                        member.setCurrentPlan(planCombo.getValue());
                        return memberService.updateMember(member.getId(), member);
                    } catch (Exception e) {
                        showError("خطأ", "فشل تحديث المشترك: " + e.getMessage());
                        return null;
                    }
                }
                return null;
            });

            Optional<Member> result = dialog.showAndWait();
            result.ifPresent(m -> {
                loadMembers();
                showSuccess("تم بنجاح", "تم تحديث بيانات المشترك");
            });
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
        }
    }

    private void showMemberDetails(Member member) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("تفاصيل المشترك");
            alert.setHeaderText(member.getName());

            // ✅ أضف CSS styling للـ alert
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

            Coach coach = member.getCoach();
            Plan plan = member.getCurrentPlan();

            alert.setContentText(
                    "رقم العضوية: " + member.getMemberId() +
                            "\nالهاتف: " + member.getPhone() +
                            "\nالجنس: " + (member.getGender() != null ? member.getGender().getArabicName() : "-") +
                            "\nالكوتش: " + (coach != null ? coach.getName() + " - " + coach.getSpecialty() : "-") +
                            "\nالخطة: " + (plan != null ? plan.getName() : "-") +
                            "\nتاريخ الانتهاء: " + (member.getSubscriptionEndDate() != null ?
                            member.getSubscriptionEndDate().format(dateFormatter) : "-")
            );

            alert.showAndWait();
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
        }
    }

    private void deleteMember(Member member) {
        try {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("تأكيد الحذف");
            confirm.setHeaderText("هل أنت متأكد من حذف " + member.getName() + "؟");
            confirm.setContentText("سيتم إخفاء المشترك ولن يظهر في القائمة");

            // ✅ أضف CSS styling للـ alert
            confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                memberService.softDeleteMember(member.getId());
                loadMembers();
                showSuccess("تم بنجاح", "تم حذف المشترك");
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