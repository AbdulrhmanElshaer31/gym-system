package com.gym.ui.controller;

import com.gym.entity.Session;
import com.gym.service.SessionService;
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
import java.util.Optional;

@Component
public class SessionsController {

    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, Long> colId;
    @FXML private TableColumn<Session, String> colName;
    @FXML private TableColumn<Session, String> colSessions;
    @FXML private TableColumn<Session, String> colPrice;
    @FXML private TableColumn<Session, String> colRenewalPrice;
    @FXML private TableColumn<Session, String> colStatus;
    @FXML private TableColumn<Session, Void> colActions;

    @Autowired
    private SessionService sessionService;

    private final ObservableList<Session> sessionsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadSessions();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colSessions.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSessionsText()));

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

        setupActionsColumn();

        sessionsTable.setItems(sessionsList);
        colId.setSortType(TableColumn.SortType.ASCENDING);
        sessionsTable.getSortOrder().setAll(colId);
    }

    private void setupActionsColumn() {
        Callback<TableColumn<Session, Void>, TableCell<Session, Void>> cellFactory = param -> new TableCell<>() {
            private final MenuButton menuButton = new MenuButton("⋮");

            {
                menuButton.getStyleClass().add("action-menu");

                MenuItem editItem = new MenuItem("تعديل");
                editItem.setOnAction(e -> editSession(getTableView().getItems().get(getIndex())));

                MenuItem toggleItem = new MenuItem("تغيير الحالة");
                toggleItem.setOnAction(e -> toggleSessionStatus(getTableView().getItems().get(getIndex())));

                MenuItem deleteItem = new MenuItem("حذف");
                deleteItem.setOnAction(e -> deleteSession(getTableView().getItems().get(getIndex())));
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

    private void loadSessions() {
        sessionsList.clear();
        var sessions = sessionService.getAllSessions();
        sessions.sort(Comparator.comparing(Session::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        sessionsList.addAll(sessions);
        sessionsTable.sort();
    }

    @FXML
    private void showAddSessionDialog() {
        Dialog<Session> dialog = new Dialog<>();
        dialog.setTitle("إضافة حصة جديدة");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = createSessionForm(null);
        dialog.getDialogPane().setContent(grid);

        TextField nameField = (TextField) grid.lookup("#nameField");
        TextField sessionsField = (TextField) grid.lookup("#sessionsField");
        TextField priceField = (TextField) grid.lookup("#priceField");
        TextField renewalPriceField = (TextField) grid.lookup("#renewalPriceField");
        TextArea descField = (TextArea) grid.lookup("#descField");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().isEmpty() || sessionsField.getText().isEmpty() || priceField.getText().isEmpty()) {
                    showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
                    return null;
                }

                try {
                    Session session = new Session();
                    session.setName(nameField.getText());
                    session.setNumberOfSessions(Integer.parseInt(sessionsField.getText()));
                    session.setPrice(new BigDecimal(priceField.getText()));

                    if (!renewalPriceField.getText().isEmpty()) {
                        session.setRenewalPrice(new BigDecimal(renewalPriceField.getText()));
                    }

                    session.setDescription(descField.getText());

                    return sessionService.createSession(session);
                } catch (NumberFormatException e) {
                    showError("خطأ", "يرجى إدخال أرقام صحيحة");
                    return null;
                }
            }
            return null;
        });

        Optional<Session> result = dialog.showAndWait();
        result.ifPresent(session -> {
            loadSessions();
            showSuccess("تم بنجاح", "تم إضافة الحصة " + session.getName());
        });
    }

    private void editSession(Session session) {
        Dialog<Session> dialog = new Dialog<>();
        dialog.setTitle("تعديل الحصة");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        ButtonType saveButtonType = new ButtonType("حفظ", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = createSessionForm(session);
        dialog.getDialogPane().setContent(grid);

        TextField nameField = (TextField) grid.lookup("#nameField");
        TextField sessionsField = (TextField) grid.lookup("#sessionsField");
        TextField priceField = (TextField) grid.lookup("#priceField");
        TextField renewalPriceField = (TextField) grid.lookup("#renewalPriceField");
        TextArea descField = (TextArea) grid.lookup("#descField");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().isEmpty() || sessionsField.getText().isEmpty() || priceField.getText().isEmpty()) {
                    showError("خطأ", "يرجى ملء جميع الحقول المطلوبة");
                    return null;
                }

                try {
                    session.setName(nameField.getText());
                    session.setNumberOfSessions(Integer.parseInt(sessionsField.getText()));
                    session.setPrice(new BigDecimal(priceField.getText()));

                    if (!renewalPriceField.getText().isEmpty()) {
                        session.setRenewalPrice(new BigDecimal(renewalPriceField.getText()));
                    }

                    session.setDescription(descField.getText());

                    return sessionService.updateSession(session.getId(), session);
                } catch (NumberFormatException e) {
                    showError("خطأ", "يرجى إدخال أرقام صحيحة");
                    return null;
                }
            }
            return null;
        });

        Optional<Session> result = dialog.showAndWait();
        result.ifPresent(s -> {
            loadSessions();
            showSuccess("تم بنجاح", "تم تحديث الحصة");
        });
    }

    private GridPane createSessionForm(Session session) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(session != null ? session.getName() : "");
        nameField.setId("nameField");
        nameField.setPromptText("اسم الحصة (مثل: حصص خاصة، حصص جماعية)");

        TextField sessionsField = new TextField(session != null ? String.valueOf(session.getNumberOfSessions()) : "");
        sessionsField.setId("sessionsField");
        sessionsField.setPromptText("عدد الحصص");

        TextField priceField = new TextField(session != null ? session.getPrice().toString() : "");
        priceField.setId("priceField");
        priceField.setPromptText("السعر");

        TextField renewalPriceField = new TextField(
                session != null && session.getRenewalPrice() != null ? session.getRenewalPrice().toString() : ""
        );
        renewalPriceField.setId("renewalPriceField");
        renewalPriceField.setPromptText("سعر التجديد (اختياري)");

        TextArea descField = new TextArea(session != null && session.getDescription() != null ? session.getDescription() : "");
        descField.setId("descField");
        descField.setPromptText("وصف الحصة");
        descField.setWrapText(true);
        descField.setPrefRowCount(3);

        Label renewalHint = new Label("ⓘ إذا تركت حقل التجديد فارغاً، سيتم استخدام السعر الأساسي");
        renewalHint.setStyle("-fx-font-size: 10; -fx-text-fill: #888;");
        renewalHint.setWrapText(true);

        // Quick selection buttons for common session counts
        HBox sessionButtons = new HBox(5);
        for (int count : new int[]{5, 10, 20}) {
            Button btn = new Button(count + " حصص");
            btn.setStyle("-fx-font-size: 10; -fx-padding: 5;");
            btn.setOnAction(e -> sessionsField.setText(String.valueOf(count)));
            sessionButtons.getChildren().add(btn);
        }

        grid.add(new Label("اسم الحصة:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("عدد الحصص:"), 0, 1);
        grid.add(sessionsField, 1, 1);
        grid.add(new Label(""), 0, 2);
        grid.add(sessionButtons, 1, 2);
        grid.add(new Label("السعر (ج.م):"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("سعر التجديد (ج.م):"), 0, 4);
        grid.add(renewalPriceField, 1, 4);
        grid.add(new Label(""), 0, 5);
        grid.add(renewalHint, 1, 5);
        grid.add(new Label("الوصف:"), 0, 6);
        grid.add(descField, 1, 6);

        return grid;
    }

    private void toggleSessionStatus(Session session) {
        sessionService.toggleSessionStatus(session.getId());
        loadSessions();
        showSuccess("تم بنجاح", "تم تغيير حالة الحصة");
    }

    private void deleteSession(Session session) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText("هل أنت متأكد من حذف حصة " + session.getName() + "؟");
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            sessionService.deleteSession(session.getId());
            loadSessions();
            showSuccess("تم بنجاح", "تم حذف الحصة");
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