package by.charity.client.controller;

import by.charity.client.network.ServerConnection;
import by.charity.client.ui.StyleManager;
import by.charity.client.ui.ValidationHelper;
import by.charity.shared.dto.Request;
import by.charity.shared.dto.Response;
import by.charity.shared.model.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

public class ProfileController {
    private final User currentUser;
    private ImageView avatarView;
    private StackPane avatarPane;
    private static final String PREF_KEY_PREFIX = "avatar_path_";

    public ProfileController(User currentUser) {
        this.currentUser = currentUser;
    }

    public javafx.scene.Node createView() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        Label header = new Label("Мой профиль");
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + StyleManager.TEXT_DARK + ";");

        HBox content = new HBox(24);
        content.setAlignment(Pos.TOP_LEFT);
        content.getChildren().addAll(buildProfileCard(), buildPasswordCard());

        root.getChildren().addAll(header, content);
        return root;
    }

    private VBox buildProfileCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(400);
        card.setStyle(
                "-fx-background-color: white;"
                        + "-fx-border-color: " + StyleManager.PINK_MAIN + ";"
                        + "-fx-border-radius: 12; -fx-background-radius: 12;"
                        + "-fx-border-width: 1.5;");

        Label title = new Label("Личные данные");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + StyleManager.TEXT_DARK + ";");

        avatarPane = buildAvatarPane();

        Label usernameLabel = new Label("Логин: " + currentUser.getUsername());
        usernameLabel.setStyle("-fx-font-size: 12px;"
                + " -fx-text-fill: " + StyleManager.TEXT_GRAY + ";");

        Label roleLabel = new Label("Роль: " + currentUser.getRole().getDisplayName());
        roleLabel.setStyle("-fx-font-size: 12px;"
                + " -fx-text-fill: " + StyleManager.TEXT_GRAY + ";");

        Separator sep = new Separator();

        Label fullNameLbl = fieldLabel("Полное имя *");
        TextField fullNameField = styledField("Полное имя", currentUser.getFullName());
        ValidationHelper.applyMaxLength(fullNameField, 100);

        Label emailLbl = fieldLabel("Email");
        TextField emailField = styledField("Email", currentUser.getEmail());
        Label emailError = errorLabel();

        Label descLbl = fieldLabel("О себе");
        TextArea descArea = new TextArea(
                currentUser.getDescription() != null
                        ? currentUser.getDescription() : "");
        descArea.setPromptText("Расскажите немного о себе...");
        descArea.setStyle(StyleManager.textFieldStyle()
                + " -fx-pref-row-count: 3;");
        descArea.setWrapText(true);

        emailField.focusedProperty().addListener((obs, was, is) -> {
            if (!is) ValidationHelper.validateEmail(emailField, emailError);
        });

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px; -fx-wrap-text: true;");

        Button saveBtn = new Button("Сохранить изменения");
        saveBtn.setStyle(StyleManager.buttonPrimary());
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(e -> {
            if (fullNameField.getText().trim().isEmpty()) {
                statusLabel.setStyle("-fx-font-size: 12px;"
                        + " -fx-text-fill: " + StyleManager.DANGER + ";");
                statusLabel.setText("Полное имя обязательно");
                return;
            }
            if (!ValidationHelper.validateEmail(emailField, emailError)) return;

            saveBtn.setDisable(true);
            CompletableFuture.supplyAsync(() -> {
                try {
                    User updated = new User();
                    updated.setId(currentUser.getId());
                    updated.setDescription(descArea.getText().trim());
                    updated.setFullName(fullNameField.getText().trim());
                    updated.setEmail(emailField.getText().trim());
                    Request req = new Request(Request.Action.UPDATE_PROFILE, null);
                    req.addParam("user", updated);
                    return ServerConnection.getInstance().send(req);
                } catch (Exception ex) {
                    return Response.error(ex.getMessage());
                }
            }).thenAcceptAsync(response -> {
                saveBtn.setDisable(false);
                if (response.isSuccess()) {
                    currentUser.setFullName(fullNameField.getText().trim());
                    currentUser.setEmail(emailField.getText().trim());
                    statusLabel.setStyle("-fx-font-size: 12px;"
                            + " -fx-text-fill: " + StyleManager.SUCCESS + ";");
                    statusLabel.setText("Данные успешно обновлены");
                } else {
                    statusLabel.setStyle("-fx-font-size: 12px;"
                            + " -fx-text-fill: " + StyleManager.DANGER + ";");
                    statusLabel.setText(response.getMessage());
                }
            }, Platform::runLater);
        });

        card.getChildren().addAll(
                title,
                avatarPane,
                usernameLabel, roleLabel,
                sep,
                fullNameLbl, fullNameField,
                emailLbl, emailField, emailError,
                statusLabel, saveBtn, descLbl, descArea);
        return card;
    }

    private StackPane buildAvatarPane() {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        pane.setPrefSize(110, 110);
        pane.setMaxSize(110, 110);

        Circle bg = new Circle(54);
        bg.setFill(Color.web(StyleManager.PINK_SOFT));
        bg.setStroke(Color.web(StyleManager.PINK_MAIN));
        bg.setStrokeWidth(2);

        String initials = getInitials(currentUser.getFullName());
        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + StyleManager.PINK_DEEP + ";");
        initialsLabel.setId("initials-label");

        avatarView = new ImageView();
        avatarView.setFitWidth(108);
        avatarView.setFitHeight(108);
        avatarView.setPreserveRatio(false);


        Circle clip = new Circle(54, 54, 54);
        avatarView.setClip(clip);
        avatarView.setVisible(false);

        Label editIcon = new Label("Изменить фото");
        editIcon.setStyle(
                "-fx-font-size: 10px;"
                        + "-fx-text-fill: white;"
                        + "-fx-background-color: rgba(0,0,0,0.45);"
                        + "-fx-padding: 3 6 3 6;"
                        + "-fx-background-radius: 8;"
                        + "-fx-cursor: hand;");
        StackPane.setAlignment(editIcon, Pos.BOTTOM_CENTER);
        StackPane.setMargin(editIcon, new Insets(0, 0, 6, 0));

        pane.getChildren().addAll(bg, initialsLabel, avatarView, editIcon);

        loadSavedAvatar();

        editIcon.setOnMouseClicked(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Выберите фото профиля");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter(
                            "Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File file = chooser.showOpenDialog(
                    editIcon.getScene().getWindow());
            if (file != null) {
                setAvatar(file.getAbsolutePath());
                Preferences prefs = Preferences.userRoot()
                        .node("charity_app");
                prefs.put(PREF_KEY_PREFIX + currentUser.getId(), file.getAbsolutePath());
            }
        });

        return pane;
    }

    private void loadSavedAvatar() {
        try {
            Preferences prefs = Preferences.userRoot().node("charity_app");
            String path = prefs.get(PREF_KEY_PREFIX + currentUser.getId(), null);
            if (path != null && new File(path).exists()) {
                setAvatar(path);
            }
        } catch (Exception ignored) {}
    }

    private void setAvatar(String filePath) {
        try {
            Image img = new Image(new FileInputStream(filePath));
            avatarView.setImage(img);
            avatarView.setVisible(true);
            avatarPane.getChildren().stream()
                    .filter(n -> "initials-label".equals(n.getId()))
                    .findFirst()
                    .ifPresent(n -> n.setVisible(false));
        } catch (Exception ignored) {}
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private VBox buildPasswordCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(380);
        card.setStyle(
                "-fx-background-color: white;"
                        + "-fx-border-color: " + StyleManager.PINK_MAIN + ";"
                        + "-fx-border-radius: 12; -fx-background-radius: 12;"
                        + "-fx-border-width: 1.5;");

        Label title = new Label("Изменение пароля");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + StyleManager.TEXT_DARK + ";");

        Separator sep = new Separator();

        PasswordField currentPass = new PasswordField();
        currentPass.setPromptText("Текущий пароль");
        currentPass.setStyle(StyleManager.textFieldStyle());

        PasswordField newPass = new PasswordField();
        newPass.setPromptText("Новый пароль (минимум 6 символов)");
        newPass.setStyle(StyleManager.textFieldStyle());

        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Повторите новый пароль");
        confirmPass.setStyle(StyleManager.textFieldStyle());

        Label passStatus = new Label();
        passStatus.setStyle("-fx-font-size: 12px; -fx-wrap-text: true;");

        Button changeBtn = new Button("Изменить пароль");
        changeBtn.setStyle(StyleManager.buttonPrimary());
        changeBtn.setMaxWidth(Double.MAX_VALUE);

        changeBtn.setOnAction(e -> {
            if (currentPass.getText().isEmpty()) {
                passStatus.setStyle("-fx-font-size: 12px;"
                        + " -fx-text-fill: " + StyleManager.DANGER + ";");
                passStatus.setText("Введите текущий пароль");
                return;
            }
            if (newPass.getText().length() < 6) {
                passStatus.setStyle("-fx-font-size: 12px;"
                        + " -fx-text-fill: " + StyleManager.DANGER + ";");
                passStatus.setText("Пароль должен содержать минимум 6 символов");
                return;
            }
            if (!newPass.getText().equals(confirmPass.getText())) {
                passStatus.setStyle("-fx-font-size: 12px;"
                        + " -fx-text-fill: " + StyleManager.DANGER + ";");
                passStatus.setText("Пароли не совпадают");
                return;
            }

            changeBtn.setDisable(true);
            CompletableFuture.supplyAsync(() -> {
                try {
                    Request req = new Request(Request.Action.CHANGE_PASSWORD, null);
                    req.addParam("userId", currentUser.getId());
                    req.addParam("oldPassword", currentPass.getText());
                    req.addParam("newPassword", newPass.getText());
                    return ServerConnection.getInstance().send(req);
                } catch (Exception ex) {
                    return Response.error(ex.getMessage());
                }
            }).thenAcceptAsync(response -> {
                changeBtn.setDisable(false);
                if (response.isSuccess()) {
                    passStatus.setStyle("-fx-font-size: 12px;"
                            + " -fx-text-fill: " + StyleManager.SUCCESS + ";");
                    passStatus.setText("Пароль успешно изменён");
                    currentPass.clear();
                    newPass.clear();
                    confirmPass.clear();
                } else {
                    passStatus.setStyle("-fx-font-size: 12px;"
                            + " -fx-text-fill: " + StyleManager.DANGER + ";");
                    passStatus.setText(response.getMessage());
                }
            }, Platform::runLater);
        });

        card.getChildren().addAll(
                title, sep,
                fieldLabel("Текущий пароль"), currentPass,
                fieldLabel("Новый пароль"), newPass,
                fieldLabel("Повторите пароль"), confirmPass,
                passStatus, changeBtn);
        return card;
    }

    private TextField styledField(String prompt, String val) {
        TextField tf = new TextField(val != null ? val : "");
        tf.setPromptText(prompt);
        tf.setStyle(StyleManager.textFieldStyle());
        return tf;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + StyleManager.TEXT_DARK + ";");
        return l;
    }

    private Label errorLabel() {
        Label l = new Label();
        l.setStyle("-fx-font-size: 11px;"
                + " -fx-text-fill: " + StyleManager.DANGER + ";");
        l.setVisible(false);
        l.setManaged(false);
        l.visibleProperty().addListener((obs, o, n) -> l.setManaged(n));
        return l;
    }

}