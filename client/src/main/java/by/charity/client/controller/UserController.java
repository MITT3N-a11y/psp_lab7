package by.charity.client.controller;

import by.charity.client.network.ServerConnection;
import by.charity.client.ui.StyleManager;
import by.charity.shared.dto.Request;
import by.charity.shared.dto.Response;
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
import javafx.scene.text.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UserController {
    private final User currentUser;
    private final ObservableList<User> users =
            FXCollections.observableArrayList();

    private static final String DARK = StyleManager.TEXT_DARK;
    private static final String GRAY = StyleManager.TEXT_GRAY;

    public UserController(User currentUser) {
        this.currentUser = currentUser;
    }

    public javafx.scene.Node createView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("Пользователи");
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Добавить пользователя");
        addBtn.setStyle(StyleManager.buttonPrimary());

        headerRow.getChildren().addAll(header, spacer, addBtn);

        TableView<User> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(headerRow, table);

        addBtn.setOnAction(e -> showUserDialog(null));
        loadUsers();
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<User> buildTable() {
        TableView<User> tv = new TableView<>(users);
        tv.setStyle(StyleManager.tableStyle());
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User, String> usernameCol =
                new TableColumn<>("Логин");
        usernameCol.setCellValueFactory(
                new PropertyValueFactory<>("username"));
        usernameCol.setCellFactory(col -> textCell(DARK, false));

        TableColumn<User, String> nameCol =
                new TableColumn<>("Полное имя");
        nameCol.setCellValueFactory(
                new PropertyValueFactory<>("fullName"));
        nameCol.setCellFactory(col -> textCell(DARK, false));

        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setCellFactory(col -> textCell(GRAY, false));

        TableColumn<User, Role> roleCol = new TableColumn<>("Роль");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Role v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v.getDisplayName());
                setStyle("-fx-text-fill: "
                        + (v == Role.ADMIN
                        ? StyleManager.PINK_DEEP : GRAY)
                        + ";");
            }
        });

        TableColumn<User, Boolean> activeCol =
                new TableColumn<>("Статус");
        activeCol.setCellValueFactory(
                new PropertyValueFactory<>("active"));
        activeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v ? "Активен" : "Неактивен");
                setStyle("-fx-text-fill: "
                        + (v ? StyleManager.SUCCESS : StyleManager.DANGER)
                        + ";");
            }
        });
        activeCol.setPrefWidth(90);

        TableColumn<User, Void> actionsCol =
                new TableColumn<>("Действия");
        actionsCol.setPrefWidth(230);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("Изменить");
            private final Button deactBtn  = new Button("Деакт.");
            private final Button deleteBtn = new Button("Удалить");
            private final HBox box =
                    new HBox(6, editBtn, deactBtn, deleteBtn);
            {
                editBtn.setStyle(StyleManager.buttonSecondary());
                deactBtn.setStyle(StyleManager.buttonSecondary());
                deleteBtn.setStyle(StyleManager.buttonDanger());
                box.setAlignment(Pos.CENTER);
                editBtn.setOnAction(e ->
                        showUserDialog(getTableView().getItems()
                                .get(getIndex())));
                deactBtn.setOnAction(e ->
                        deactivateUser(getTableView().getItems()
                                .get(getIndex())));
                deleteBtn.setOnAction(e ->
                        deleteUser(getTableView().getItems()
                                .get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        tv.getColumns().addAll(usernameCol, nameCol, emailCol,
                roleCol, activeCol, actionsCol);
        return tv;
    }

    private void loadUsers() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return ServerConnection.getInstance().send(
                        new Request(Request.Action.GET_ALL_USERS, null));
            } catch (Exception e) {
                return Response.error(e.getMessage());
            }
        }).thenAcceptAsync(response -> {
            if (response.isSuccess())
                users.setAll((List<User>) response.getData());
            else showAlert("Ошибка", response.getMessage());
        }, Platform::runLater);
    }

    private void deactivateUser(User user) {
        if (user.getId().equals(currentUser.getId())) {
            showAlert("Ошибка",
                    "Нельзя деактивировать собственную учётную запись.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Деактивировать пользователя «"
                        + user.getUsername() + "»?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            CompletableFuture.supplyAsync(() -> {
                try {
                    Request req = new Request(
                            Request.Action.DEACTIVATE_USER, null);
                    req.addParam("id", user.getId());
                    return ServerConnection.getInstance().send(req);
                } catch (Exception e) {
                    return Response.error(e.getMessage());
                }
            }).thenAcceptAsync(response -> {
                if (response.isSuccess()) loadUsers();
                else showAlert("Ошибка", response.getMessage());
            }, Platform::runLater);
        });
    }

    private void deleteUser(User user) {
        if (user.getId().equals(currentUser.getId())) {
            showAlert("Ошибка",
                    "Нельзя удалить собственную учётную запись.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Полностью удалить пользователя «"
                        + user.getUsername()
                        + "» из базы данных? Это действие необратимо.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            CompletableFuture.supplyAsync(() -> {
                try {
                    Request req = new Request(
                            Request.Action.DELETE_USER, null);
                    req.addParam("id", user.getId());
                    return ServerConnection.getInstance().send(req);
                } catch (Exception e) {
                    return Response.error(e.getMessage());
                }
            }).thenAcceptAsync(response -> {
                if (response.isSuccess()) loadUsers();
                else showAlert("Ошибка", response.getMessage());
            }, Platform::runLater);
        });
    }

    private void showUserDialog(User existing) {
        boolean isNew = existing == null;
        User user = isNew ? new User() : existing;

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(isNew
                ? "Новый пользователь" : "Редактировать пользователя");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setMinWidth(380);

        TextField usernameField = styledField("Логин *",
                user.getUsername());
        usernameField.setDisable(!isNew);
        TextField fullNameField = styledField("Полное имя *",
                user.getFullName());
        TextField emailField = styledField("Email", user.getEmail());
        ComboBox<Role> roleCombo = new ComboBox<>(
                FXCollections.observableArrayList(Role.values()));
        roleCombo.setStyle(StyleManager.textFieldStyle());
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setValue(
                user.getRole() != null ? user.getRole() : Role.GUEST);

        content.getChildren().addAll(
                lbl("Логин *"), usernameField,
                lbl("Полное имя *"), fullNameField,
                lbl("Email"), emailField,
                lbl("Роль"), roleCombo
        );

        if (isNew) {
            Label hint = new Label("Начальный пароль: changeme123");
            hint.setStyle("-fx-font-size: 11px; -fx-text-fill: " + GRAY + ";");
            content.getChildren().add(hint);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle(
                "-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            user.setUsername(usernameField.getText().trim());
            user.setFullName(fullNameField.getText().trim());
            user.setEmail(emailField.getText().trim());
            user.setRole(roleCombo.getValue());
            return user;
        });

        dialog.showAndWait().ifPresent(u ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Request req = isNew
                                ? new Request(
                                Request.Action.CREATE_USER, null)
                                : new Request(
                                Request.Action.UPDATE_USER, null);
                        req.addParam("user", u);
                        return ServerConnection.getInstance().send(req);
                    } catch (Exception e) {
                        return Response.error(e.getMessage());
                    }
                }).thenAcceptAsync(response -> {
                    if (response.isSuccess()) loadUsers();
                    else showAlert("Ошибка", response.getMessage());
                }, Platform::runLater));
    }

    private <T> TableCell<User, T> textCell(String color, boolean bold) {
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

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}