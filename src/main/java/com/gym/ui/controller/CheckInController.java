package com.gym.ui.controller;

import com.gym.entity.Member;
import com.gym.entity.Plan;
import com.gym.service.MemberService;
import com.gym.service.PlanService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class CheckInController {

    @FXML private TextField searchField;
    @FXML private Button btnSearch;

    // Member info section
    @FXML private VBox memberInfoBox;
    @FXML private Label lblMemberId;
    @FXML private Label lblMemberName;
    @FXML private Label lblPhone;
    @FXML private Label lblGender;
    @FXML private Label lblCoach;
    @FXML private Label lblPlan;
    @FXML private Label lblStatus;
    @FXML private Label lblSubscriptionEnd;
    @FXML private Label lblRemainingSession;
    @FXML private Label lblTotalSession;
    @FXML private Label lblSessionsStatus;

    // Actions section
    @FXML private VBox actionsBox;
    @FXML private Button btnRecordAttendance;
    @FXML private Button btnRenewSubscription;
    @FXML private Button btnDeleteMember;

    @Autowired
    private MemberService memberService;

    @Autowired
    private PlanService planService;

    private Member currentMember;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        memberInfoBox.setVisible(false);
        actionsBox.setVisible(false);

        btnSearch.setOnAction(e -> searchMember());
        btnRecordAttendance.setOnAction(e -> recordAttendance());
        btnRenewSubscription.setOnAction(e -> showRenewDialog());
        btnDeleteMember.setOnAction(e -> deleteMember());

        // Allow search on Enter key
        searchField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                searchMember();
            }
        });
    }

    @FXML
    private void searchMember() {
        String searchId = searchField.getText().trim();

        if (searchId.isEmpty()) {
            showError("خطأ", "يرجى إدخال معرف المشترك");
            return;
        }

        try {
            // ✅ بدون تحقق من الحالة - يعرض أي حال كانت
            currentMember = memberService.findByMemberIdWithValidation(searchId);
            displayMemberInfo();
            memberInfoBox.setVisible(true);
            actionsBox.setVisible(true);
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
            memberInfoBox.setVisible(false);
            actionsBox.setVisible(false);
        }
    }

    private void displayMemberInfo() {
        Plan plan = currentMember.getCurrentPlan();

        lblMemberId.setText(currentMember.getMemberId());
        lblMemberName.setText(currentMember.getName());
        lblPhone.setText(currentMember.getPhone());
        lblGender.setText(currentMember.getGender() != null ? currentMember.getGender().getArabicName() : "-");
        lblCoach.setText(currentMember.getCoach() != null ? currentMember.getCoach().getName() : "-");
        lblPlan.setText(plan != null ? plan.getName() : "-");

        // ✅ Subscription Status - مرتبط بالحصص والأيام
        String status = currentMember.getStatus().getArabicName();
        String color = currentMember.getStatus().getColor();
        lblStatus.setText(status);
        lblStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");

        lblSubscriptionEnd.setText(currentMember.getSubscriptionEndDate().format(dateFormatter));

        // Sessions info with status
        if (plan != null && plan.getNumberOfSessions() != null && plan.getNumberOfSessions() > 0) {
            lblRemainingSession.setText(String.valueOf(currentMember.getRemainingSession()));
            lblTotalSession.setText(String.valueOf(plan.getNumberOfSessions()));

            // Show sessions status
            Member.SessionsStatus sessionsStatus = currentMember.getSessionsStatus();
            lblSessionsStatus.setText(sessionsStatus.getArabicName());
            lblSessionsStatus.setStyle("-fx-text-fill: " + sessionsStatus.getColor() + "; -fx-font-weight: bold;");

            // Color for remaining sessions
            if (currentMember.getRemainingSession() <= 5 && currentMember.getRemainingSession() > 0) {
                lblRemainingSession.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 14;");
            } else if (currentMember.getRemainingSession() <= 0) {
                lblRemainingSession.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14;");
            } else {
                lblRemainingSession.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 14;");
            }
        } else {
            lblRemainingSession.setText("بلا حد");
            lblTotalSession.setText("بلا حد");
            lblSessionsStatus.setText("بدون حد");
            lblSessionsStatus.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void recordAttendance() {
        // ✅ Check if member status is expired
        if (currentMember.getStatus() == Member.SubscriptionStatus.EXPIRED) {
            showWarning("تنبيه", "انتهى الاشتراك!\nيرجى تجديد الاشتراك");
            return;
        }

        // Check if plan has sessions limit
        if (currentMember.getCurrentPlan() == null ||
                currentMember.getCurrentPlan().getNumberOfSessions() == null ||
                currentMember.getCurrentPlan().getNumberOfSessions() == 0) {
            showError("خطأ", "هذه الخطة بدون حصص محددة");
            return;
        }

        // Check if sessions available
        if (currentMember.getRemainingSession() <= 0) {
            showWarning("تنبيه", "انتهت الحصص!\nيرجى تجديد الاشتراك");
            return;
        }

        try {
            memberService.recordAttendance(currentMember.getId());
            showSuccess("تم بنجاح", "تم تسجيل الحضور ✓");

            // Refresh display
            searchMember();
        } catch (Exception e) {
            showError("خطأ", e.getMessage());
        }
    }

    @FXML
    private void showRenewDialog() {
        Dialog<Plan> dialog = new Dialog<>();
        dialog.setTitle("تجديد الاشتراك");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        ButtonType renewButtonType = new ButtonType("تجديد", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(renewButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<Plan> planCombo = new ComboBox<>();
        planCombo.setItems(javafx.collections.FXCollections.observableArrayList(planService.getAllPlans()));
        planCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Plan item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " - " + item.getPrice() + " ج.م");
            }
        });
        planCombo.setButtonCell(planCombo.getCellFactory().call(null));

        grid.add(new Label("اختر الخطة الجديدة:"), 0, 0);
        grid.add(planCombo, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == renewButtonType) {
                return planCombo.getValue();
            }
            return null;
        });

        Optional<Plan> result = dialog.showAndWait();
        result.ifPresent(selectedPlan -> {
            try {
                memberService.renewSubscription(currentMember.getId(), selectedPlan);
                showSuccess("تم بنجاح", "تم تجديد الاشتراك ✓");
                searchMember();
            } catch (Exception e) {
                showError("خطأ", e.getMessage());
            }
        });
    }

    @FXML
    private void deleteMember() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText("هل أنت متأكد من حذف المشترك " + currentMember.getName() + "؟");
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                memberService.softDeleteMember(currentMember.getId());
                showSuccess("تم بنجاح", "تم حذف المشترك ✓");
                searchField.clear();
                memberInfoBox.setVisible(false);
                actionsBox.setVisible(false);
            } catch (Exception e) {
                showError("خطأ", e.getMessage());
            }
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

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        alert.showAndWait();
    }
}