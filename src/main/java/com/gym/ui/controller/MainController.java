package com.gym.ui.controller;

import com.gym.SpringContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    @FXML private BorderPane mainContainer;
    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;

    @FXML private Button btnCheckIn;      // ✅ NEW - الزر الجديد
    @FXML private Button btnDashboard;
    @FXML private Button btnMembers;
    @FXML private Button btnCoaches;
    @FXML private Button btnPlans;
    @FXML private Button btnFinancial;
    @FXML private Button btnProducts;
    @FXML private Button btnInventory;
    @FXML private Button btnSettings;

    @FXML private Label lblCurrentPage;
    @FXML private Label lblLogoText;
    @FXML private Label lblLogoSubtitle;

    private Button currentActiveButton;

    @FXML
    public void initialize() {
        loadSavedGymName();
        // ✅ الصفحة الديفولت هي Check-in
        loadView("تسجيل الحضور والحصص", "/fxml/CheckInView.fxml");
        setActiveButton(btnCheckIn);
    }

    private void loadSavedGymName() {
        try {
            String gymName = SettingsController.getSavedGymName();
            if (gymName != null && !gymName.isEmpty() && lblLogoText != null) {
                lblLogoText.setText("💪 " + gymName);
            }
        } catch (Exception ignored) {}
    }

    public void updateGymName(String gymName) {
        if (lblLogoText != null && gymName != null && !gymName.isEmpty()) {
            lblLogoText.setText("💪 " + gymName);
        }
    }

    // ✅ Check In
    @FXML
    private void showCheckIn() {
        loadView("تسجيل الحضور والحصص", "/fxml/CheckInView.fxml");
        setActiveButton(btnCheckIn);
    }

    @FXML
    private void showDashboard() {
        loadView("لوحة التحكم", "/fxml/DashboardView.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    private void showMembers() {
        loadView("إدارة المشتركين", "/fxml/MembersView.fxml");
        setActiveButton(btnMembers);
    }

    @FXML
    private void showCoaches() {
        loadView("إدارة المدربين", "/fxml/CoachesView.fxml");
        setActiveButton(btnCoaches);
    }

    @FXML
    private void showPlans() {
        loadView("إدارة الخطط", "/fxml/PlansView.fxml");
        setActiveButton(btnPlans);
    }

    @FXML
    private void showFinancial() {
        loadView("الإدارة المالية", "/fxml/FinancialView.fxml");
        setActiveButton(btnFinancial);
    }

    @FXML
    private void showProducts() {
        loadView("المشروبات والمأكولات", "/fxml/ProductsView.fxml");
        setActiveButton(btnProducts);
    }

    @FXML
    private void showInventory() {
        loadView("الجرد", "/fxml/InventoryView.fxml");
        setActiveButton(btnInventory);
    }

    @FXML
    private void showSettings() {
        loadView("الإعدادات", "/fxml/SettingsView.fxml");
        setActiveButton(btnSettings);
    }

    private void loadView(String title, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(SpringContext::getBean);

            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            lblCurrentPage.setText(title);
        } catch (Exception e) {
            e.printStackTrace();
            showError("خطأ في تحميل الصفحة", fxmlPath + "\n" + e.getMessage());
        }
    }

    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("nav-button-active");
        }
        button.getStyleClass().add("nav-button-active");
        currentActiveButton = button;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void applyDarkTheme() {
        if (mainContainer != null) {
            mainContainer.getStylesheets().clear();
            mainContainer.getStylesheets()
                    .add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        }
    }

    public void applyLightTheme() {
        if (mainContainer != null) {
            mainContainer.getStylesheets().clear();
        }
    }

    public void applyFontSize(String size) {
        double fontSize = switch (size) {
            case "صغير" -> 12;
            case "كبير" -> 16;
            default -> 14;
        };
        mainContainer.setStyle("-fx-font-size: " + fontSize + "px;");
    }
}