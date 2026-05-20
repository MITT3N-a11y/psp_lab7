package by.charity.client.controller;

import by.charity.client.network.ServerConnection;
import by.charity.client.ui.StyleManager;
import by.charity.client.ui.ValidationHelper;
import by.charity.shared.dto.Request;
import by.charity.shared.dto.Response;
import by.charity.shared.model.CharityFund;
import by.charity.shared.model.Role;
import by.charity.shared.model.User;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FundController {
    private final User currentUser;
    private final ObservableList<CharityFund> funds =
            FXCollections.observableArrayList();

    private static final String DARK = StyleManager.TEXT_DARK;
    private static final String GRAY = StyleManager.TEXT_GRAY;

    public FundController(User currentUser) {
        this.currentUser = currentUser;
    }

    public javafx.scene.Node createView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("Благотворительные фонды");
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean canCreate = currentUser.getRole() == Role.ADMIN
                || currentUser.getRole() == Role.MANAGER;
        Button addBtn = new Button("+ Добавить фонд");
        addBtn.setStyle(StyleManager.buttonPrimary());
        addBtn.setVisible(canCreate);
        addBtn.setManaged(canCreate);

        headerRow.getChildren().addAll(header, spacer, addBtn);

        TableView<CharityFund> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(headerRow, table);

        addBtn.setOnAction(e -> showFundDialog(null));
        loadFunds();
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<CharityFund> buildTable() {
        TableView<CharityFund> tv = new TableView<>(funds);
        tv.setStyle(StyleManager.tableStyle());
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<CharityFund, String> nameCol =
                new TableColumn<>("Название");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(col -> textCell(DARK, false));

        TableColumn<CharityFund, String> regCol =
                new TableColumn<>("Рег. номер");
        regCol.setCellValueFactory(
                new PropertyValueFactory<>("registrationNumber"));
        regCol.setCellFactory(col -> textCell(GRAY, false));
        regCol.setPrefWidth(130);

        TableColumn<CharityFund, BigDecimal> recCol =
                new TableColumn<>("Собрано (BYN)");
        recCol.setCellValueFactory(
                new PropertyValueFactory<>("totalReceived"));
        recCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal val,
                                                boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : formatByn(val));
                setStyle("-fx-text-fill: " + StyleManager.SUCCESS + ";"
                        + " -fx-font-weight: bold;");
            }
        });

        TableColumn<CharityFund, BigDecimal> spentCol =
                new TableColumn<>("Израсходовано (BYN)");
        spentCol.setCellValueFactory(
                new PropertyValueFactory<>("totalSpent"));
        spentCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal val,
                                                boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : formatByn(val));
                setStyle("-fx-text-fill: " + DARK + ";");
            }
        });

        TableColumn<CharityFund, Boolean> activeCol =
                new TableColumn<>("Статус");
        activeCol.setCellValueFactory(
                new PropertyValueFactory<>("active"));
        activeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); return; }
                setText(val ? "Активен" : "Неактивен");
                setStyle("-fx-text-fill: "
                        + (val ? StyleManager.SUCCESS : StyleManager.DANGER)
                        + ";");
            }
        });
        activeCol.setPrefWidth(90);

        TableColumn<CharityFund, Void> actionsCol =
                new TableColumn<>("Действия");
        actionsCol.setPrefWidth(290);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn    = new Button("Изменить");
            private final Button expenseBtn = new Button("+ Расход");
            private final Button deactBtn   = new Button("Деакт.");
            private final Button deleteBtn  = new Button("Удалить");
            private final HBox box =
                    new HBox(6, editBtn, expenseBtn, deactBtn, deleteBtn);
            {
                editBtn.setStyle(StyleManager.buttonSecondary());
                expenseBtn.setStyle(StyleManager.buttonSecondary());
                deactBtn.setStyle(StyleManager.buttonSecondary());
                deleteBtn.setStyle(StyleManager.buttonDanger());
                box.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e ->
                        showFundDialog(getTableView().getItems()
                                .get(getIndex())));
                expenseBtn.setOnAction(e ->
                        showExpenseDialog(getTableView().getItems()
                                .get(getIndex())));
                deactBtn.setOnAction(e ->
                        deactivateFund(getTableView().getItems()
                                .get(getIndex())));
                deleteBtn.setOnAction(e ->
                        deleteFund(getTableView().getItems()
                                .get(getIndex())));

                boolean canEdit  = currentUser.getRole() == Role.ADMIN
                        || currentUser.getRole() == Role.MANAGER;
                boolean canAdmin = currentUser.getRole() == Role.ADMIN;

                editBtn.setVisible(canEdit);
                editBtn.setManaged(canEdit);
                expenseBtn.setVisible(canEdit);
                expenseBtn.setManaged(canEdit);
                deactBtn.setVisible(canAdmin);
                deactBtn.setManaged(canAdmin);
                deleteBtn.setVisible(canAdmin);
                deleteBtn.setManaged(canAdmin);
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        tv.getColumns().addAll(nameCol, regCol, recCol, spentCol,
                activeCol, actionsCol);
        return tv;
    }

    private void loadFunds() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return ServerConnection.getInstance().send(
                        new Request(Request.Action.GET_ALL_FUNDS, null));
            } catch (Exception e) {
                return Response.error(e.getMessage());
            }
        }).thenAcceptAsync(response -> {
            if (response.isSuccess())
                funds.setAll((List<CharityFund>) response.getData());
            else showAlert("Ошибка", response.getMessage());
        }, Platform::runLater);
    }

    private void deactivateFund(CharityFund fund) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Деактивировать фонд «" + fund.getName() + "»?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            CompletableFuture.supplyAsync(() -> {
                try {
                    Request req = new Request(
                            Request.Action.DEACTIVATE_FUND, null);
                    req.addParam("id", fund.getId());
                    return ServerConnection.getInstance().send(req);
                } catch (Exception e) {
                    return Response.error(e.getMessage());
                }
            }).thenAcceptAsync(response -> {
                if (response.isSuccess()) loadFunds();
                else showAlert("Ошибка", response.getMessage());
            }, Platform::runLater);
        });
    }

    private void deleteFund(CharityFund fund) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Полностью удалить фонд «" + fund.getName()
                        + "» из базы данных?\n"
                        + "Все проекты, пожертвования и отчёты этого фонда "
                        + "также будут удалены.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            CompletableFuture.supplyAsync(() -> {
                try {
                    Request req = new Request(
                            Request.Action.DELETE_FUND, null);
                    req.addParam("id", fund.getId());
                    return ServerConnection.getInstance().send(req);
                } catch (Exception e) {
                    return Response.error(e.getMessage());
                }
            }).thenAcceptAsync(response -> {
                if (response.isSuccess()) loadFunds();
                else showAlert("Ошибка", response.getMessage());
            }, Platform::runLater);
        });
    }

    private void showFundDialog(CharityFund existing) {
        boolean isNew = existing == null;
        CharityFund fund = isNew ? new CharityFund() : existing;

        Dialog<CharityFund> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Новый фонд" : "Редактировать фонд");
        dialog.setHeaderText(null);

        VBox content = new VBox(8);
        content.setPadding(new Insets(20));
        content.setMinWidth(400);

        TextField nameField  = styledField("Название *", fund.getName());
        Label nameError      = errorLabel();
        TextField descField  = styledField("Описание", fund.getDescription());
        TextField regField   = styledField("Рег. номер",
                fund.getRegistrationNumber());
        TextField emailField = styledField("Email", fund.getContactEmail());
        Label emailError     = errorLabel();
        TextField phoneField = styledField("Телефон", fund.getContactPhone());
        Label phoneError     = errorLabel();

        emailField.focusedProperty().addListener((obs, was, is) -> {
            if (!is) ValidationHelper.validateEmail(emailField, emailError);
        });
        phoneField.focusedProperty().addListener((obs, was, is) -> {
            if (!is) ValidationHelper.validatePhone(phoneField, phoneError);
        });

        content.getChildren().addAll(
                lbl("Название *"), nameField, nameError,
                lbl("Описание"), descField,
                lbl("Рег. номер"), regField,
                lbl("Email"), emailField, emailError,
                lbl("Телефон"), phoneField, phoneError
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle(
                "-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        Button okBtn = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean valid = true;
            if (!ValidationHelper.validateRequired(
                    nameField, nameError, "Название")) valid = false;
            if (!ValidationHelper.validateEmail(emailField, emailError))
                valid = false;
            if (!ValidationHelper.validatePhone(phoneField, phoneError))
                valid = false;
            if (!valid) ev.consume();
        });

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            fund.setName(nameField.getText().trim());
            fund.setDescription(descField.getText().trim());
            fund.setRegistrationNumber(regField.getText().trim());
            fund.setContactEmail(emailField.getText().trim());
            fund.setContactPhone(phoneField.getText().trim());
            return fund;
        });

        dialog.showAndWait().ifPresent(f ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Request req = isNew
                                ? new Request(
                                Request.Action.CREATE_FUND, null)
                                : new Request(
                                Request.Action.UPDATE_FUND, null);
                        req.addParam("fund", f);
                        return ServerConnection.getInstance().send(req);
                    } catch (Exception e) {
                        return Response.error(e.getMessage());
                    }
                }).thenAcceptAsync(response -> {
                    if (response.isSuccess()) loadFunds();
                    else showAlert("Ошибка", response.getMessage());
                }, Platform::runLater));
    }

    private <T> TableCell<CharityFund, T> textCell(String color,
                                                   boolean bold) {
        return new TableCell<>() {
            @Override protected void updateItem(T val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : val.toString());
                setStyle("-fx-text-fill: " + color + ";"
                        + (bold ? " -fx-font-weight: bold;" : ""));
            }
        };
    }

    private TextField styledField(String prompt, String val) {
        TextField tf = new TextField(val != null ? val : "");
        tf.setPromptText(prompt);
        tf.setStyle(StyleManager.textFieldStyle());
        return tf;
    }

    private Label lbl(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");
        return l;
    }

    private Label errorLabel() {
        Label l = new Label();
        l.setStyle("-fx-font-size: 11px;"
                + " -fx-text-fill: " + StyleManager.DANGER + ";");
        l.setVisible(false);
        l.setManaged(false);
        l.visibleProperty().addListener(
                (obs, o, n) -> l.setManaged(n));
        return l;
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showExpenseDialog(CharityFund fund) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Добавить расход");
        dialog.setHeaderText(null);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setMinWidth(340);

        Label fundLabel = new Label("Фонд: " + fund.getName());
        fundLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        fundLabel.setTextFill(Color.web(DARK));

        TextField amountField = styledField("Сумма расхода (BYN)", "");
        ValidationHelper.applyMoneyFilter(amountField);
        Label errorLabel = errorLabel();

        content.getChildren().addAll(
                fundLabel,
                lbl("Сумма расхода (BYN) *"),
                amountField,
                errorLabel
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle(
                "-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        Button okBtn = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (!ValidationHelper.validateMoney(
                    amountField, errorLabel, "Сумма")) {
                ev.consume();
            }
        });

        dialog.setResultConverter(bt -> bt == ButtonType.OK
                ? Boolean.TRUE : null);

        dialog.showAndWait().ifPresent(ok -> {
            java.math.BigDecimal amount =
                    ValidationHelper.parseMoney(amountField);
            CompletableFuture.supplyAsync(() -> {
                try {
                    Request req = new Request(
                            Request.Action.ADD_FUND_EXPENSE, null);
                    req.addParam("fundId", fund.getId());
                    req.addParam("amount", amount);
                    return ServerConnection.getInstance().send(req);
                } catch (Exception e) {
                    return Response.error(e.getMessage());
                }
            }).thenAcceptAsync(response -> {
                if (response.isSuccess()) {
                    loadFunds();
                    showAlert2("Готово",
                            "Расход " + amount + " BYN успешно добавлен.");
                } else showAlert("Ошибка", response.getMessage());
            }, Platform::runLater);
        });
    }

    private void showAlert2(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private String formatByn(BigDecimal val) {
        if (val == null) return "0,00 Br";
        return String.format("%,.2f Br", val)
                .replace(',', ' ').replace('.', ',');
    }
}