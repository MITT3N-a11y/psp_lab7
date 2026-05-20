package by.charity.client.controller;

import by.charity.client.network.ServerConnection;
import by.charity.client.ui.StyleManager;
import by.charity.client.ui.ValidationHelper;
import by.charity.shared.dto.Request;
import by.charity.shared.dto.Response;
import by.charity.shared.model.Role;
import by.charity.shared.model.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LoginController {
    private final Stage stage;

    public LoginController(Stage stage) {
        this.stage = stage;
    }

    public Scene createScene() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, "
                + StyleManager.PINK_LIGHT + ", " + StyleManager.PINK_SOFT + ");");

        VBox card = new VBox(0);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(440);
        card.setStyle("""
                -fx-background-color: white;
                -fx-border-radius: 20;
                -fx-background-radius: 20;
                -fx-effect: dropshadow(gaussian, rgba(233,109,163,0.2), 20, 0, 0, 8);
                """);

        // Вкладки Вход / Регистрация
        HBox tabs = new HBox(0);
        tabs.setMaxWidth(Double.MAX_VALUE);

        Button loginTab = new Button("Вход");
        Button registerTab = new Button("Регистрация");

        String activeTab = String.format("""
                -fx-background-color: white;
                -fx-text-fill: %s;
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-padding: 14 0 14 0;
                -fx-background-radius: 0;
                -fx-border-width: 0 0 2 0;
                -fx-border-color: %s;
                -fx-cursor: hand;
                """, StyleManager.PINK_DEEP, StyleManager.PINK_DEEP);

        String inactiveTab = String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-size: 14px;
                -fx-padding: 14 0 14 0;
                -fx-background-radius: 0;
                -fx-border-width: 0;
                -fx-cursor: hand;
                """, StyleManager.PINK_LIGHT, StyleManager.TEXT_GRAY);

        loginTab.setStyle(activeTab);
        registerTab.setStyle(inactiveTab);
        loginTab.setMaxWidth(Double.MAX_VALUE);
        registerTab.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loginTab, Priority.ALWAYS);
        HBox.setHgrow(registerTab, Priority.ALWAYS);
        tabs.getChildren().addAll(loginTab, registerTab);

        // Логотип
        VBox logoBox = new VBox(6);
        logoBox.setAlignment(Pos.CENTER);
        logoBox.setPadding(new Insets(24, 20, 16, 20));
        Text heart = new Text("♥");
        heart.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        heart.setFill(Color.web(StyleManager.PINK_DEEP));
        Label title = new Label("Учёт пожертвований");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(StyleManager.TEXT_DARK));
        Label subtitle = new Label("Система прозрачной отчётности");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web(StyleManager.TEXT_GRAY));
        logoBox.getChildren().addAll(heart, title, subtitle);

        // Панель входа
        VBox loginPane = buildLoginPane();
        // Панель регистрации
        VBox registerPane = buildRegisterPane();
        registerPane.setVisible(false);
        registerPane.setManaged(false);

        loginTab.setOnAction(e -> {
            loginTab.setStyle(activeTab);
            registerTab.setStyle(inactiveTab);
            loginPane.setVisible(true);
            loginPane.setManaged(true);
            registerPane.setVisible(false);
            registerPane.setManaged(false);
        });

        registerTab.setOnAction(e -> {
            registerTab.setStyle(activeTab);
            loginTab.setStyle(inactiveTab);
            registerPane.setVisible(true);
            registerPane.setManaged(true);
            loginPane.setVisible(false);
            loginPane.setManaged(false);
        });

        card.getChildren().addAll(tabs, logoBox, loginPane, registerPane);
        root.getChildren().add(card);
        return new Scene(root, 850, 620);
    }

    private VBox buildLoginPane() {
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(10, 32, 32, 32));

        Label userLabel = fieldLabel("Имя пользователя");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Введите логин");
        usernameField.setStyle(StyleManager.textFieldStyle());

        Label passLabel = fieldLabel("Пароль");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Введите пароль");
        passwordField.setStyle(StyleManager.textFieldStyle());

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web(StyleManager.DANGER));
        errorLabel.setFont(Font.font("Segoe UI", 12));
        errorLabel.setWrapText(true);

        Button loginBtn = new Button("Войти");
        loginBtn.setStyle(StyleManager.buttonPrimary());
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> doLogin(
                usernameField.getText().trim(),
                passwordField.getText(),
                loginBtn, errorLabel));
        passwordField.setOnAction(e -> loginBtn.fire());

        pane.getChildren().addAll(
                userLabel, usernameField,
                passLabel, passwordField,
                errorLabel, loginBtn);
        return pane;
    }

    private VBox buildRegisterPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10, 32, 32, 32));

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Полное имя");
        fullNameField.setStyle(StyleManager.textFieldStyle());
        ValidationHelper.applyMaxLength(fullNameField, 100);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Логин");
        usernameField.setStyle(StyleManager.textFieldStyle());
        ValidationHelper.applyMaxLength(usernameField, 50);

        TextField emailField = new TextField();
        emailField.setPromptText("Email (необязательно)");
        emailField.setStyle(StyleManager.textFieldStyle());

        PasswordField passField = new PasswordField();
        passField.setPromptText("Пароль (минимум 6 символов)");
        passField.setStyle(StyleManager.textFieldStyle());

        PasswordField pass2Field = new PasswordField();
        pass2Field.setPromptText("Повторите пароль");
        pass2Field.setStyle(StyleManager.textFieldStyle());

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web(StyleManager.DANGER));
        errorLabel.setFont(Font.font("Segoe UI", 12));
        errorLabel.setWrapText(true);

        Button registerBtn = new Button("Зарегистрироваться");
        registerBtn.setStyle(StyleManager.buttonPrimary());
        registerBtn.setMaxWidth(Double.MAX_VALUE);

        registerBtn.setOnAction(e -> {
            // Валидация
            boolean valid = true;
            if (fullNameField.getText().trim().isEmpty()) {
                errorLabel.setText("Введите полное имя");
                fullNameField.setStyle(StyleManager.textFieldError());
                valid = false;
            } else fullNameField.setStyle(StyleManager.textFieldStyle());

            if (usernameField.getText().trim().isEmpty()) {
                errorLabel.setText("Введите логин");
                usernameField.setStyle(StyleManager.textFieldError());
                valid = false;
            } else usernameField.setStyle(StyleManager.textFieldStyle());

            if (!emailField.getText().trim().isEmpty()) {
                Label tmpLabel = new Label();
                if (!ValidationHelper.validateEmail(emailField, tmpLabel)) {
                    errorLabel.setText(tmpLabel.getText());
                    valid = false;
                }
            }

            if (passField.getText().length() < 6) {
                errorLabel.setText("Пароль должен содержать минимум 6 символов");
                passField.setStyle(StyleManager.textFieldError());
                valid = false;
            } else passField.setStyle(StyleManager.textFieldStyle());

            if (!passField.getText().equals(pass2Field.getText())) {
                errorLabel.setText("Пароли не совпадают");
                pass2Field.setStyle(StyleManager.textFieldError());
                valid = false;
            } else pass2Field.setStyle(StyleManager.textFieldStyle());

            if (!valid) return;

            User newUser = new User();
            newUser.setUsername(usernameField.getText().trim());
            newUser.setFullName(fullNameField.getText().trim());
            newUser.setEmail(emailField.getText().trim());
            newUser.setRole(Role.GUEST);
            newUser.setPasswordHash(
                    by.charity.shared.util.PasswordUtil.hashPassword(passField.getText()));
            newUser.setActive(true);

            registerBtn.setDisable(true);
            registerBtn.setText("Регистрация...");

            CompletableFuture.supplyAsync(() -> {
                try {
                    ServerConnection conn = ServerConnection.getInstance();
                    if (!conn.isConnected()) conn.connect("localhost", 8080);
                    Request req = new Request(Request.Action.REGISTER_GUEST, null);
                    req.addParam("user", newUser);
                    return conn.send(req);
                } catch (Exception ex) {
                    return Response.error(ex.getMessage());
                }
            }).thenAcceptAsync(response -> {
                registerBtn.setDisable(false);
                registerBtn.setText("Зарегистрироваться");
                if (response.isSuccess()) {
                    errorLabel.setTextFill(Color.web(StyleManager.SUCCESS));
                    errorLabel.setText("✓ Регистрация успешна! Войдите в систему.");
                    fullNameField.clear();
                    usernameField.clear();
                    emailField.clear();
                    passField.clear();
                    pass2Field.clear();
                } else {
                    errorLabel.setTextFill(Color.web(StyleManager.DANGER));
                    errorLabel.setText(response.getMessage());
                }
            }, Platform::runLater);
        });

        pane.getChildren().addAll(
                fieldLabel("Полное имя *"), fullNameField,
                fieldLabel("Логин *"), usernameField,
                fieldLabel("Email"), emailField,
                fieldLabel("Пароль *"), passField,
                fieldLabel("Повторите пароль *"), pass2Field,
                errorLabel, registerBtn);
        return pane;
    }

    private void doLogin(String username, String password, Button loginBtn, Label errorLabel) {
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Пожалуйста, заполните все поля.");
            return;
        }
        loginBtn.setDisable(true);
        loginBtn.setText("Подключение...");
        errorLabel.setText("");

        CompletableFuture.supplyAsync(() -> {
            try {
                ServerConnection conn = ServerConnection.getInstance();
                if (!conn.isConnected()) conn.connect("localhost", 8080);
                Request req = new Request(Request.Action.LOGIN, null);
                req.addParam("username", username);
                req.addParam("password", password);
                return conn.send(req);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage());
            }
        }).thenAcceptAsync(response -> {
            if (response.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.getData();
                String token = (String) data.get("token");
                User user = (User) data.get("user");
                ServerConnection.getInstance().setSessionToken(token);
                MainController mainController = new MainController(stage, user);
                stage.setScene(mainController.createScene());
                stage.setTitle("Учёт благотворительных пожертвований — " + user.getFullName());
                stage.setMaximized(true);
            } else {
                errorLabel.setText(response.getMessage());
                loginBtn.setDisable(false);
                loginBtn.setText("Войти");
            }
        }, Platform::runLater);
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        l.setTextFill(Color.web(StyleManager.TEXT_DARK));
        return l;
    }
}