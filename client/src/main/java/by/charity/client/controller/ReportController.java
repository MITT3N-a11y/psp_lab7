package by.charity.client.controller;

import by.charity.client.network.ServerConnection;
import by.charity.client.ui.StyleManager;
import by.charity.shared.dto.Request;
import by.charity.shared.dto.Response;
import by.charity.shared.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReportController {
    private final User currentUser;
    private final ObservableList<Report> reports = FXCollections.observableArrayList();
    private List<CharityFund> fundList;

    public ReportController(User currentUser) {
        this.currentUser = currentUser;
    }

    public javafx.scene.Node createView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label header = new Label("Отчёты");
        header.setStyle(StyleManager.headerLabel());
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button genBtn = new Button("+ Сформировать отчёт");
        genBtn.setStyle(StyleManager.buttonPrimary());
        boolean canGenerate = currentUser.getRole() != Role.GUEST;
        genBtn.setVisible(canGenerate);
        genBtn.setManaged(canGenerate);

        headerRow.getChildren().addAll(header, spacer, genBtn);

        if (currentUser.getRole() == Role.GUEST) {
            Label hint = new Label(
                    "Здесь отображаются публичные отчёты о расходовании средств фондов.");
            hint.setFont(Font.font("Segoe UI", 13));
            hint.setTextFill(Color.web(StyleManager.TEXT_DARK));
            root.getChildren().add(hint);
        }

        TableView<Report> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(headerRow, table);

        genBtn.setOnAction(e -> showGenerateDialog());
        loadData();
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<Report> buildTable() {
        TableView<Report> tv = new TableView<>(reports);
        tv.setStyle(StyleManager.tableStyle());
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Report, String> titleCol = new TableColumn<>("Название");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Report, Report.ReportType> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Report.ReportType v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.getDisplayName());
                setTextFill(Color.web(StyleManager.TEXT_DARK));
            }
        });
        typeCol.setPrefWidth(110);

        TableColumn<Report, String> fundCol = new TableColumn<>("Фонд");
        fundCol.setCellValueFactory(new PropertyValueFactory<>("fundName"));

        TableColumn<Report, BigDecimal> recCol = new TableColumn<>("Получено (BYN)");
        recCol.setCellValueFactory(new PropertyValueFactory<>("totalReceived"));
        recCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : formatByn(v));
                setTextFill(Color.web(StyleManager.SUCCESS));
                setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            }
        });

        TableColumn<Report, Integer> countCol = new TableColumn<>("Пожертвований");
        countCol.setCellValueFactory(new PropertyValueFactory<>("donationsCount"));
        countCol.setPrefWidth(110);

        TableColumn<Report, LocalDate> periodCol = new TableColumn<>("Период");
        periodCol.setCellValueFactory(p ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        p.getValue().getPeriodStart()));
        periodCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                Report r = getTableView().getItems().get(getIndex());
                setText(r.getPeriodStart() + " – " + r.getPeriodEnd());
                setTextFill(Color.web(StyleManager.TEXT_DARK));
            }
        });

        TableColumn<Report, Boolean> visibilityCol = new TableColumn<>("Видимость");
        visibilityCol.setCellValueFactory(new PropertyValueFactory<>("public"));
        visibilityCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); return; }
                setText(val ? "Публичный" : "Приватный");
                setTextFill(val
                        ? Color.web(StyleManager.SUCCESS)
                        : Color.web(StyleManager.TEXT_GRAY));
            }
        });
        visibilityCol.setPrefWidth(100);

        tv.getColumns().addAll(titleCol, typeCol, fundCol, recCol,
                countCol, periodCol, visibilityCol);
        return tv;
    }

    private void loadData() {
        CompletableFuture.supplyAsync(() -> {
            try {
                Request.Action action = currentUser.getRole() == Role.GUEST
                        ? Request.Action.GET_PUBLIC_REPORTS
                        : Request.Action.GET_ALL_REPORTS;
                Response r1 = ServerConnection.getInstance().send(
                        new Request(action, null));
                Response r2 = ServerConnection.getInstance().send(
                        new Request(Request.Action.GET_ALL_FUNDS, null));
                return new Response[]{r1, r2};
            } catch (Exception e) { return null; }
        }).thenAcceptAsync(responses -> {
            if (responses != null) {
                if (responses[0].isSuccess())
                    reports.setAll((List<Report>) responses[0].getData());
                if (responses[1].isSuccess())
                    fundList = (List<CharityFund>) responses[1].getData();
            }
        }, Platform::runLater);
    }

    private void showGenerateDialog() {
        if (fundList == null || fundList.isEmpty()) {
            showAlert("Нет фондов", "Сначала создайте хотя бы один фонд.");
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Формирование отчёта");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setMinWidth(420);

        ComboBox<CharityFund> fundCombo = new ComboBox<>(
                FXCollections.observableArrayList(fundList));
        fundCombo.setStyle(StyleManager.textFieldStyle());
        fundCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Report.ReportType> typeCombo = new ComboBox<>(
                FXCollections.observableArrayList(Report.ReportType.values()));
        typeCombo.setStyle(StyleManager.textFieldStyle());
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.getSelectionModel().selectFirst();

        DatePicker startPicker = new DatePicker(LocalDate.now().withDayOfMonth(1));
        DatePicker endPicker   = new DatePicker(LocalDate.now());
        startPicker.setStyle(StyleManager.textFieldStyle());
        endPicker.setStyle(StyleManager.textFieldStyle());

        TextField notesField = styledField("Примечания", "");

        CheckBox publicCheck = new CheckBox(
                "Публичный отчёт (виден всем пользователям)");
        publicCheck.setTextFill(Color.web(StyleManager.TEXT_DARK));
        publicCheck.setSelected(false);

        content.getChildren().addAll(
                lbl("Фонд *"), fundCombo,
                lbl("Тип отчёта"), typeCombo,
                lbl("Период с"), startPicker,
                lbl("Период по"), endPicker,
                lbl("Примечания"), notesField,
                publicCheck
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialogPane(dialog.getDialogPane());

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? Boolean.TRUE : null);

        dialog.showAndWait().ifPresent(ok -> {
            if (fundCombo.getValue() == null) {
                showAlert("Ошибка", "Выберите фонд");
                return;
            }
            CompletableFuture.supplyAsync(() -> {
                try {
                    Request req = new Request(Request.Action.GENERATE_REPORT, null);
                    req.addParam("fundId",    fundCombo.getValue().getId());
                    req.addParam("projectId", null);
                    req.addParam("type",      typeCombo.getValue());
                    req.addParam("start",     startPicker.getValue());
                    req.addParam("end",       endPicker.getValue());
                    req.addParam("notes",     notesField.getText().trim());
                    req.addParam("isPublic",  publicCheck.isSelected());
                    return ServerConnection.getInstance().send(req);
                } catch (Exception e) { return Response.error(e.getMessage()); }
            }).thenAcceptAsync(response -> {
                if (response.isSuccess()) {
                    loadData();
                    showInfo("Отчёт сформирован", "Отчёт успешно создан и сохранён.");
                } else showAlert("Ошибка", response.getMessage());
            }, Platform::runLater);
        });
    }

    private TextField styledField(String prompt, String val) {
        TextField tf = new TextField(val);
        tf.setPromptText(prompt);
        tf.setStyle(StyleManager.textFieldStyle());
        return tf;
    }

    private Label lbl(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        l.setTextFill(Color.web(StyleManager.TEXT_DARK));
        return l;
    }

    private void styleDialogPane(DialogPane dp) {
        dp.setStyle("-fx-background-color: " + StyleManager.PINK_LIGHT + ";");
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private String formatByn(BigDecimal val) {
        return String.format("%,.2f Br", val).replace(',', ' ').replace('.', ',');
    }
}