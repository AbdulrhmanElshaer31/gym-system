package com.gym.ui.controller;

import com.gym.service.BackupService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

@Component
public class SettingsController {

    @Autowired
    private BackupService backupService;

    @Autowired(required = false)
    private MainController mainController;

    @FXML private TextField txtGymName;
    @FXML private ComboBox<String> cmbCurrency;
    @FXML private CheckBox chkAutoBackup;
    @FXML private TextField txtBackupPath;
    @FXML private Spinner<Integer> spnExpiryDays;
    @FXML private Spinner<Integer> spnLowStockUnits;
    @FXML private ComboBox<String> cmbTheme;
    @FXML private ComboBox<String> cmbFontSize;

    private static final String SETTINGS_FILE = "gym-settings.properties";

    public static String getSavedGymName() {
        Properties props = new Properties();
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                props.load(fis);
                return props.getProperty("gymName", "");
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    @FXML


    private void setupCurrencyComboBox() {
        if (cmbCurrency != null) {
            cmbCurrency.getItems().addAll(
                    "جنيه مصري (ج.م)",
                    "دولار أمريكي ($)",
                    "ريال سعودي (ر.س)",
                    "درهم إماراتي (د.إ)"
            );
            cmbCurrency.setValue("جنيه مصري (ج.م)");
        }
    }

    private void setupThemeComboBox() {
        if (cmbTheme != null) {
            cmbTheme.getItems().addAll("الوضع الفاتح", "الوضع الداكن");
            cmbTheme.setValue("الوضع الفاتح");
            cmbTheme.setOnAction(e -> applyTheme());
        }
    }

    private void setupFontSizeComboBox() {
        if (cmbFontSize != null) {
            cmbFontSize.getItems().addAll("صغير", "متوسط", "كبير");
            cmbFontSize.setValue("كبير");
            cmbFontSize.setOnAction(e -> applyFontSize());
        }
    }

    private void setupSpinners() {
        if (spnExpiryDays != null) {
            spnExpiryDays.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 7));
        }
        if (spnLowStockUnits != null) {
            spnLowStockUnits.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 5));
        }
    }

    private void loadSettings() {
        Properties props = new Properties();
        File settingsFile = new File(SETTINGS_FILE);

        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                props.load(fis);

                txtGymName.setText(props.getProperty("gymName", ""));
                txtBackupPath.setText(props.getProperty("backupPath", "./backups"));

                String currency = props.getProperty("currency", "جنيه مصري (ج.م)");
                if (cmbCurrency.getItems().contains(currency)) cmbCurrency.setValue(currency);

                chkAutoBackup.setSelected(Boolean.parseBoolean(props.getProperty("autoBackup", "true")));

                spnExpiryDays.getValueFactory().setValue(Integer.parseInt(props.getProperty("expiryDays", "7")));
                spnLowStockUnits.getValueFactory().setValue(Integer.parseInt(props.getProperty("lowStockUnits", "5")));

                String theme = props.getProperty("theme", "الوضع الفاتح");
                if (cmbTheme.getItems().contains(theme)) {
                    cmbTheme.setValue(theme);
                    applyTheme();
                }

                String fontSize = props.getProperty("fontSize", "كبير");
                if (cmbFontSize.getItems().contains(fontSize)) {
                    cmbFontSize.setValue(fontSize);
                    applyFontSize();
                }

            } catch (Exception e) {
                txtBackupPath.setText("./backups");
            }
        } else {
            txtBackupPath.setText("./backups");
        }
    }

    @FXML
    private void saveGeneralSettings() {
        Properties props = new Properties();
        File settingsFile = new File(SETTINGS_FILE);

        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                props.load(fis);
            } catch (Exception e) { /* ignore */ }
        }

        props.setProperty("gymName", txtGymName.getText());
        props.setProperty("currency", cmbCurrency.getValue());
        props.setProperty("backupPath", txtBackupPath.getText());
        props.setProperty("autoBackup", String.valueOf(chkAutoBackup.isSelected()));
        props.setProperty("expiryDays", String.valueOf(spnExpiryDays.getValue()));
        props.setProperty("lowStockUnits", String.valueOf(spnLowStockUnits.getValue()));

        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "Gym Management System Settings - © 2026");
            if (mainController != null && !txtGymName.getText().isEmpty()) {
                mainController.updateGymName(txtGymName.getText());
            }
            showSuccess("تم الحفظ", "تم حفظ الإعدادات العامة بنجاح");
        } catch (Exception e) {
            showError("خطأ", "فشل حفظ الإعدادات: " + e.getMessage());
        }
    }

    @FXML
    private void changeBackupPath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("اختر مجلد النسخ الاحتياطي");

        // تعيين المجلد الحالي كنقطة بداية
        String currentPath = txtBackupPath.getText();
        File currentDir = new File(currentPath);
        if (currentDir.exists() && currentDir.isDirectory()) {
            directoryChooser.setInitialDirectory(currentDir);
        }

        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            txtBackupPath.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void createBackup() {
        try {
            String backupDir = txtBackupPath.getText();

            // التحقق من صحة المسار
            if (backupDir == null || backupDir.trim().isEmpty()) {
                showError("خطأ", "يرجى تحديد مسار النسخ الاحتياطي أولاً");
                return;
            }

            // إظهار مؤشر التحميل
            Alert waitAlert = new Alert(Alert.AlertType.INFORMATION);
            waitAlert.setTitle("جاري الإنشاء");
            waitAlert.setHeaderText(null);
            waitAlert.setContentText("جاري إنشاء النسخة الاحتياطية، يرجى الانتظار...");
            waitAlert.show();

            // إنشاء النسخة الاحتياطية
            String backupPath = backupService.createBackup(backupDir);

            waitAlert.close();

            // حذف النسخ القديمة (الاحتفاظ بآخر 10 نسخ)
            backupService.cleanOldBackups(backupDir, 10);

            showSuccess("تم النسخ الاحتياطي",
                    "تم إنشاء نسخة احتياطية بنجاح:\n" + backupPath);

        } catch (Exception e) {
            e.printStackTrace();
            showError("خطأ", "فشل إنشاء النسخة الاحتياطية:\n" + e.getMessage());
        }
    }

    @FXML
    private void restoreBackup() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("اختر ملف النسخة الاحتياطية");

        // تعيين المجلد الحالي كنقطة بداية
        String currentPath = txtBackupPath.getText();
        File currentDir = new File(currentPath);
        if (currentDir.exists() && currentDir.isDirectory()) {
            fileChooser.setInitialDirectory(currentDir);
        }

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Backup Files", "*.zip")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("تأكيد الاستعادة");
            confirm.setHeaderText("استعادة النسخة الاحتياطية");
            confirm.setContentText(
                    "⚠️ تحذير: سيتم استبدال جميع البيانات الحالية!\n\n" +
                            "هل أنت متأكد من رغبتك في استعادة النسخة الاحتياطية؟\n" +
                            "ملف النسخة: " + selectedFile.getName()
            );

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // إظهار مؤشر التحميل
                        Alert waitAlert = new Alert(Alert.AlertType.INFORMATION);
                        waitAlert.setTitle("جاري الاستعادة");
                        waitAlert.setHeaderText(null);
                        waitAlert.setContentText("جاري استعادة النسخة الاحتياطية، يرجى الانتظار...");
                        waitAlert.show();

                        backupService.restoreBackup(selectedFile.getAbsolutePath());

                        waitAlert.close();

                        showSuccess("تم الاستعادة",
                                "تم استعادة النسخة الاحتياطية بنجاح!\n\n" +
                                        "⚠️ مهم: يجب إعادة تشغيل البرنامج الآن لتفعيل التغييرات");

                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("خطأ", "فشل استعادة النسخة الاحتياطية:\n" + e.getMessage());
                    }
                }
            });
        }
    }

    @FXML
    private void contactUs() {
        showInfo("تواصل معنا",
                "للدعم الفني والاستفسارات:\n\n" +
                        "الموبايل: 01023143535\n\n" +
                        "البريد الإلكتروني:\nabdelrhmanelshaer31@gmail.com");
    }

    @FXML
    private void showUserGuide() {
        showInfo("دليل الاستخدام",
                "نظام إدارة الجيم - دليل سريع\n\n" +
                        "1. المشتركين: إضافة وإدارة الأعضاء\n" +
                        "2. الخطط: تحديد خطط الاشتراك\n" +
                        "3. المالية: متابعة الإيرادات والمصروفات\n" +
                        "4. المنتجات: إدارة المخزون والمبيعات\n" +
                        "5. الجرد: مراجعة حركات المخزون\n" +
                        "6. الإعدادات: تخصيص النظام والنسخ الاحتياطي");
    }

    private void applyTheme() {
        if (cmbTheme == null || mainController == null) return;
        if ("الوضع الداكن".equals(cmbTheme.getValue())) mainController.applyDarkTheme();
        else mainController.applyLightTheme();
    }

    private void applyFontSize() {
        if (cmbFontSize == null || mainController == null) return;
        mainController.applyFontSize(cmbFontSize.getValue());
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    public void initialize() {
        setupCurrencyComboBox();
        setupThemeComboBox();
        setupFontSizeComboBox();
        setupSpinners();
        loadSettings();

        // طباعة معلومات قاعدة البيانات للتأكد
        if (backupService != null) {
            backupService.printDatabaseInfo();
        }
    }
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}