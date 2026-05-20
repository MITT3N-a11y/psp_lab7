package by.charity.client.controller;

import by.charity.client.ui.StyleManager;
import by.charity.shared.model.Role;
import by.charity.shared.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

public class MainController {
    private final Stage stage;
    private final User currentUser;
    private BorderPane root;
    private VBox sidebar;
    private Button activeNavBtn = null;

    public MainController(Stage stage, User currentUser) {
        this.stage = stage;
        this.currentUser = currentUser;
    }

    public Scene createScene() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        buildSidebar();
        buildTopBar();
        showDashboard();

        return new Scene(root, 1280, 760);
    }

    private void buildTopBar() {
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(12, 24, 12, 24));
        topBar.setStyle("-fx-background-color: white;"
                + "-fx-effect: dropshadow(gaussian, rgba(233,109,163,0.1), 4, 0, 0, 2);");

        Label userInfo = new Label(currentUser.getFullName()
                + "   |   " + currentUser.getRole().getDisplayName());
        userInfo.setFont(Font.font("Segoe UI", 13));
        userInfo.setTextFill(Color.web(StyleManager.TEXT_DARK));

        Button logoutBtn = new Button("Выйти");
        logoutBtn.setStyle(StyleManager.buttonSecondary());
        logoutBtn.setOnAction(e -> {
            try {
                by.charity.client.network.ServerConnection.getInstance()
                        .send(new by.charity.shared.dto.Request(
                                by.charity.shared.dto.Request.Action.LOGOUT, null));
            } catch (Exception ignored) {}
            stage.setScene(new LoginController(stage).createScene());
            stage.setMaximized(false);
            stage.setWidth(850);
            stage.setHeight(620);
            stage.centerOnScreen();
        });

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(spacer, userInfo, logoutBtn);
        root.setTop(topBar);
    }

    private void buildSidebar() {
        sidebar = new VBox(0);
        sidebar.setStyle(StyleManager.sidebarStyle());
        sidebar.setPrefWidth(230);

        VBox logoArea = new VBox(4);
        logoArea.setAlignment(Pos.CENTER);
        logoArea.setPadding(new Insets(24, 10, 24, 10));
        logoArea.setStyle("-fx-background-color: rgba(0,0,0,0.1);");

        Label logo = new Label("Пожертвования");
        logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        logo.setTextFill(Color.WHITE);

        logoArea.getChildren().add(logo);
        sidebar.getChildren().add(logoArea);
        sidebar.getChildren().add(new Separator());

        addNavSection("Главное");
        Button dashBtn = addNavButton("Дашборд", this::showDashboard);
        activeNavBtn = dashBtn;
        dashBtn.setStyle(StyleManager.sidebarButtonActive());

        addNavSection("Управление");
        addNavButton("Фонды", this::showFunds);
        addNavButton("Проекты", this::showProjects);
        addNavButton("Пожертвования", this::showDonations);

        addNavSection("Отчётность");
        addNavButton("Отчёты", this::showReports);

        if (currentUser.getRole() == Role.ADMIN) {
            addNavSection("Администрирование");
            addNavButton("Пользователи", this::showUsers);
        }

        addNavSection("Аккаунт");
        addNavButton("Мой профиль", this::showProfile);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Label version = new Label("v1.0.0  |  2026");
        version.setFont(Font.font("Segoe UI", 11));
        version.setTextFill(Color.web("#ffffff80"));
        version.setPadding(new Insets(12));
        sidebar.getChildren().add(version);

        root.setLeft(sidebar);
    }

    private void addNavSection(String title) {
        Label section = new Label(title.toUpperCase());
        section.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        section.setTextFill(Color.web("#ffffff80"));
        section.setPadding(new Insets(16, 20, 4, 20));
        sidebar.getChildren().add(section);
    }

    private Button addNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle(StyleManager.sidebarButton());
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setOnAction(e -> {
            if (activeNavBtn != null) activeNavBtn.setStyle(StyleManager.sidebarButton());
            btn.setStyle(StyleManager.sidebarButtonActive());
            activeNavBtn = btn;
            action.run();
        });
        btn.setOnMouseEntered(e -> {
            if (btn != activeNavBtn) {
                btn.setStyle(StyleManager.sidebarButton().replace(
                        "-fx-background-color: transparent;",
                        "-fx-background-color: rgba(255,255,255,0.12);"));
            }
        });
        btn.setOnMouseExited(e -> {
            if (btn != activeNavBtn) btn.setStyle(StyleManager.sidebarButton());
        });

        sidebar.getChildren().add(btn);
        return btn;
    }

    private void setContent(javafx.scene.Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        root.setCenter(scroll);
    }

    private void showDashboard() {
        setContent(new DashboardController(currentUser).createView());
    }

    private void showFunds() {
        setContent(new FundController(currentUser).createView());
    }

    private void showProjects() {
        setContent(new ProjectController(currentUser).createView());
    }

    private void showDonations() {
        setContent(new DonationController(currentUser).createView());
    }

    private void showReports() {
        setContent(new ReportController(currentUser).createView());
    }

    private void showUsers() {
        setContent(new UserController(currentUser).createView());
    }

    private void showProfile() {
        setContent(new ProfileController(currentUser).createView());
    }
}