package by.charity.client.controller;

import by.charity.client.network.ServerConnection;
import by.charity.client.ui.StyleManager;
import by.charity.client.ui.ValidationHelper;
import by.charity.shared.dto.Request;
import by.charity.shared.dto.Response;
import by.charity.shared.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

public class DonationController {
    private final User currentUser;
    private final ObservableList<Donation> donations =
            FXCollections.observableArrayList();
    private List<CharityFund> fundList;
    private List<Project> projectList;

    private static final String DARK  = StyleManager.TEXT_DARK;
    private static final String GRAY  = StyleManager.TEXT_GRAY;
    private static final String PINK  = StyleManager.PINK_DEEP;

    public DonationController(User currentUser) {
        this.currentUser = currentUser;
    }

    public javafx.scene.Node createView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("Пожертвования");
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Топ-донаторы — доступно всем
        Button topBtn = new Button("Топ-3 донатёра");
        topBtn.setStyle(StyleManager.buttonSecondary());

        // Добавить пожертвование — доступно всем авторизованным
        Button addBtn = new Button("+ Зарегистрировать пожертвование");
        addBtn.setStyle(StyleManager.buttonPrimary());

        headerRow.getChildren().addAll(header, spacer, topBtn, addBtn);

        Label hint = new Label(
                "Нажмите на строку с пожертвованием чтобы увидеть подробности о доноре.");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GRAY + ";");

        TableView<Donation> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        root.getChildren().addAll(headerRow, hint, table);

        addBtn.setOnAction(e -> showDonationDialog());
        topBtn.setOnAction(e -> showTopDonors());

        loadData();
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<Donation> buildTable() {
        TableView<Donation> tv = new TableView<>(donations);
        tv.setStyle(StyleManager.tableStyle());
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setRowFactory(t -> {
            TableRow<Donation> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 1 && !row.isEmpty()) {
                    showDonorInfo(row.getItem());
                }
            });
            return row;
        });

        TableColumn<Donation, String> dateCol = new TableColumn<>("Дата");
        dateCol.setPrefWidth(130);
        dateCol.setCellValueFactory(p -> {
            if (p.getValue().getDonatedAt() == null)
                return new javafx.beans.property.SimpleStringProperty("—");
            return new javafx.beans.property.SimpleStringProperty(
                    p.getValue().getDonatedAt().format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        });
        dateCol.setCellFactory(col -> textCell(DARK));

        TableColumn<Donation, String> donorCol = new TableColumn<>("Донор");
        donorCol.setCellValueFactory(p ->
                new javafx.beans.property.SimpleStringProperty(
                        p.getValue().isAnonymous()
                                ? "Анонимно" : p.getValue().getDonorName()));
        donorCol.setCellFactory(col -> textCell(DARK));

        TableColumn<Donation, BigDecimal> amountCol =
                new TableColumn<>("Сумма (BYN)");
        amountCol.setCellValueFactory(
                new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal val,
                                                boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); return; }
                setText(formatByn(val));
                setStyle("-fx-text-fill: " + PINK + ";"
                        + " -fx-font-weight: bold; -fx-font-size: 13px;");
            }
        });
        amountCol.setPrefWidth(130);

        TableColumn<Donation, String> fundCol = new TableColumn<>("Фонд");
        fundCol.setCellValueFactory(
                new PropertyValueFactory<>("fundName"));
        fundCol.setCellFactory(col -> textCell(DARK));

        TableColumn<Donation, String> projectCol =
                new TableColumn<>("Проект");
        projectCol.setCellValueFactory(p ->
                new javafx.beans.property.SimpleStringProperty(
                        p.getValue().getProjectName() != null
                                ? p.getValue().getProjectName() : "—"));
        projectCol.setCellFactory(col -> textCell(GRAY));

        TableColumn<Donation, Donation.PaymentMethod> methodCol =
                new TableColumn<>("Способ оплаты");
        methodCol.setCellValueFactory(
                new PropertyValueFactory<>("paymentMethod"));
        methodCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(
                    Donation.PaymentMethod val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : val.getDisplayName());
                setStyle("-fx-text-fill: " + DARK + ";");
            }
        });
        methodCol.setPrefWidth(130);

        // Кнопка удаления — только для ADMIN
        if (currentUser.getRole() == Role.ADMIN) {
            TableColumn<Donation, Void> delCol =
                    new TableColumn<>("Удалить");
            delCol.setPrefWidth(90);
            delCol.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("Удалить");
                {
                    btn.setStyle(StyleManager.buttonDanger());
                    btn.setOnAction(e -> deleteDonation(
                            getTableView().getItems().get(getIndex())));
                }
                @Override protected void updateItem(Void v, boolean empty) {
                    super.updateItem(v, empty);
                    setGraphic(empty ? null : btn);
                }
            });
            tv.getColumns().addAll(
                    dateCol, donorCol, amountCol,
                    fundCol, projectCol, methodCol, delCol);
        } else {
            tv.getColumns().addAll(
                    dateCol, donorCol, amountCol,
                    fundCol, projectCol, methodCol);
        }
        return tv;
    }

    /** Показывает карточку донора при клике на строку */
    private void showDonorInfo(Donation donation) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Информация о доноре");
        dialog.setResizable(false);

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(32));
        content.setMinWidth(360);
        content.setStyle("-fx-background-color: " + StyleManager.PINK_LIGHT
                + ";");

        // Аватарка донора — ищем по имени донора в Preferences
        StackPane avatarPane = new StackPane();
        avatarPane.setPrefSize(90, 90);
        Circle bg = new Circle(44);
        bg.setFill(Color.web(StyleManager.PINK_SOFT));
        bg.setStroke(Color.web(StyleManager.PINK_MAIN));
        bg.setStrokeWidth(2);

        String donorDisplayName = donation.isAnonymous()
                ? "Анонимно" : donation.getDonorName();
        String initials = getInitials(donorDisplayName);
        Label initialsLbl = new Label(initials);
        initialsLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + PINK + ";");

        avatarPane.getChildren().addAll(bg, initialsLbl);

        // Если пожертвование зарегистрировано пользователем системы —
        // пытаемся найти его аватарку по registeredByUserId
        if (donation.getRegisteredByUserId() != null) {
            try {
                Preferences prefs = Preferences.userRoot()
                        .node("charity_app");
                String avatarPath = prefs.get(
                        "avatar_path_" + donation.getRegisteredByUserId(),
                        null);
                if (avatarPath != null
                        && new java.io.File(avatarPath).exists()) {
                    javafx.scene.image.ImageView iv =
                            new javafx.scene.image.ImageView(
                                    new javafx.scene.image.Image(
                                            new java.io.FileInputStream(
                                                    avatarPath)));
                    iv.setFitWidth(88);
                    iv.setFitHeight(88);
                    iv.setPreserveRatio(false);
                    Circle clip = new Circle(44, 44, 44);
                    iv.setClip(clip);
                    initialsLbl.setVisible(false);
                    avatarPane.getChildren().add(iv);
                }
            } catch (Exception ignored) {}
        }

        String name = donation.isAnonymous()
                ? "Анонимно" : donation.getDonorName();

        // Имя
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        VBox detailsBox = new VBox(8);
        detailsBox.setStyle(StyleManager.card());
        detailsBox.setPadding(new Insets(14));
        detailsBox.setMaxWidth(280);

        if (!donation.isAnonymous()) {
            if (donation.getDonorEmail() != null
                    && !donation.getDonorEmail().isBlank()) {
                detailsBox.getChildren().add(
                        infoRow("Email:", donation.getDonorEmail()));
            }
        }
        detailsBox.getChildren().addAll(
                infoRow("Сумма:", formatByn(donation.getAmount())),
                infoRow("Способ оплаты:",
                        donation.getPaymentMethod().getDisplayName()),
                infoRow("Фонд:", donation.getFundName() != null
                        ? donation.getFundName() : "—"),
                infoRow("Проект:", donation.getProjectName() != null
                        ? donation.getProjectName() : "—")
        );

        if (donation.getComment() != null
                && !donation.getComment().isBlank()) {
            Label commentTitle = new Label("Комментарий:");
            commentTitle.setStyle("-fx-font-size: 12px;"
                    + " -fx-font-weight: bold; -fx-text-fill: " + GRAY + ";");
            Label commentText = new Label(donation.getComment());
            commentText.setStyle("-fx-font-size: 13px;"
                    + " -fx-text-fill: " + DARK + ";");
            commentText.setWrapText(true);
            detailsBox.getChildren().addAll(commentTitle, commentText);
        }

        if (donation.isAnonymous()) {
            Label anonLbl = new Label("Это пожертвование анонимное.");
            anonLbl.setStyle("-fx-font-size: 12px;"
                    + " -fx-text-fill: " + GRAY + "; -fx-font-style: italic;");
            content.getChildren().addAll(avatarPane, nameLbl,
                    anonLbl, detailsBox);
        } else {
            content.getChildren().addAll(avatarPane, nameLbl, detailsBox);
        }

        Button closeBtn = new Button("Закрыть");
        closeBtn.setStyle(StyleManager.buttonSecondary());
        closeBtn.setOnAction(e -> dialog.close());
        content.getChildren().add(closeBtn);

        dialog.setScene(new Scene(content));
        dialog.showAndWait();
    }

    /** Показывает топ-3 донатёра */
    @SuppressWarnings("unchecked")
    private void showTopDonors() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return ServerConnection.getInstance().send(
                        new Request(Request.Action.GET_TOP_DONORS, null));
            } catch (Exception e) {
                return Response.error(e.getMessage());
            }
        }).thenAcceptAsync(response -> {
            if (!response.isSuccess()) {
                showAlert("Ошибка", response.getMessage());
                return;
            }
            List<Map<String, Object>> top =
                    (List<Map<String, Object>>) response.getData();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Топ-3 донатёра");
            dialog.setResizable(false);

            VBox content = new VBox(16);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(28));
            content.setMinWidth(380);
            content.setStyle("-fx-background-color: "
                    + StyleManager.PINK_LIGHT + ";");

            Label title = new Label("Топ-3 донатёра");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;"
                    + " -fx-text-fill: " + DARK + ";");

            if (top.isEmpty()) {
                Label empty = new Label(
                        "Ещё нет именных пожертвований.");
                empty.setStyle("-fx-font-size: 13px;"
                        + " -fx-text-fill: " + GRAY + ";");
                content.getChildren().addAll(title, empty);
            } else {
                String[] medals = {"1", "2", "3"};
                for (int i = 0; i < top.size(); i++) {
                    Map<String, Object> entry = top.get(i);
                    VBox card = new VBox(6);
                    card.setStyle(StyleManager.card());
                    card.setPadding(new Insets(14));
                    card.setMaxWidth(320);

                    HBox topLine = new HBox(12);
                    topLine.setAlignment(Pos.CENTER_LEFT);

                    Label medal = new Label(medals[i]);
                    medal.setStyle("-fx-font-size: 22px;"
                            + " -fx-font-weight: bold;"
                            + " -fx-text-fill: " + PINK + ";");

                    Label donorName = new Label(
                            (String) entry.get("donorName"));
                    donorName.setStyle("-fx-font-size: 15px;"
                            + " -fx-font-weight: bold;"
                            + " -fx-text-fill: " + DARK + ";");

                    topLine.getChildren().addAll(medal, donorName);

                    BigDecimal total =
                            (BigDecimal) entry.get("totalAmount");
                    long count = ((Number) entry.get("count")).longValue();

                    Label details = new Label(
                            formatByn(total) + "  |  "
                                    + count + " пожертв.");
                    details.setStyle("-fx-font-size: 13px;"
                            + " -fx-text-fill: " + GRAY + ";");

                    card.getChildren().addAll(topLine, details);
                    content.getChildren().add(card);
                }
            }

            Button closeBtn = new Button("Закрыть");
            closeBtn.setStyle(StyleManager.buttonSecondary());
            closeBtn.setOnAction(e -> dialog.close());
            content.getChildren().addAll(new Separator(), closeBtn);

            dialog.setScene(new Scene(content));
            dialog.showAndWait();
        }, Platform::runLater);
    }

    private void deleteDonation(Donation donation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Полностью удалить это пожертвование из базы данных?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            CompletableFuture.supplyAsync(() -> {
                try {
                    Request req = new Request(
                            Request.Action.DELETE_DONATION, null);
                    req.addParam("id", donation.getId());
                    return ServerConnection.getInstance().send(req);
                } catch (Exception e) {
                    return Response.error(e.getMessage());
                }
            }).thenAcceptAsync(response -> {
                if (response.isSuccess()) loadData();
                else showAlert("Ошибка", response.getMessage());
            }, Platform::runLater);
        });
    }

    private void loadData() {
        CompletableFuture.supplyAsync(() -> {
            try {
                Response r1 = ServerConnection.getInstance().send(
                        new Request(
                                Request.Action.GET_ALL_DONATIONS, null));
                Response r2 = ServerConnection.getInstance().send(
                        new Request(Request.Action.GET_ALL_FUNDS, null));
                Response r3 = ServerConnection.getInstance().send(
                        new Request(
                                Request.Action.GET_ALL_PROJECTS, null));
                return new Response[]{r1, r2, r3};
            } catch (Exception e) { return null; }
        }).thenAcceptAsync(responses -> {
            if (responses == null) return;
            if (responses[0].isSuccess())
                donations.setAll(
                        (List<Donation>) responses[0].getData());
            if (responses[1].isSuccess())
                fundList = (List<CharityFund>) responses[1].getData();
            if (responses[2].isSuccess())
                projectList = (List<Project>) responses[2].getData();
        }, Platform::runLater);
    }

    private void showDonationDialog() {
        if (fundList == null || fundList.isEmpty()) {
            showAlert("Нет фондов",
                    "Сначала создайте хотя бы один фонд.");
            return;
        }

        Dialog<Donation> dialog = new Dialog<>();
        dialog.setTitle("Регистрация пожертвования");

        VBox content = new VBox(8);
        content.setPadding(new Insets(20));
        content.setMinWidth(420);

        ComboBox<CharityFund> fundCombo = new ComboBox<>(
                FXCollections.observableArrayList(fundList));
        fundCombo.setStyle(StyleManager.textFieldStyle());
        fundCombo.setMaxWidth(Double.MAX_VALUE);
        fundCombo.setPromptText("Выберите фонд");

        ComboBox<Project> projectCombo = new ComboBox<>();
        projectCombo.setStyle(StyleManager.textFieldStyle());
        projectCombo.setMaxWidth(Double.MAX_VALUE);
        projectCombo.setPromptText("(не выбрано)");

        fundCombo.setOnAction(e -> {
            CharityFund sel = fundCombo.getValue();
            if (sel != null && projectList != null) {
                projectCombo.setItems(FXCollections.observableArrayList(
                        projectList.stream()
                                .filter(p -> p.getFundId()
                                        .equals(sel.getId()))
                                .toList()));
            }
        });

        TextField donorField = styledField("Имя донора *", "");
        Label donorError = errorLabel();
        TextField emailField = styledField("Email донора", "");
        Label emailError = errorLabel();
        TextField amountField = styledField("Сумма (BYN) *", "");
        Label amountError = errorLabel();
        ValidationHelper.applyMoneyFilter(amountField);

        emailField.focusedProperty().addListener((obs, was, is) -> {
            if (!is) ValidationHelper.validateEmail(emailField, emailError);
        });
        amountField.focusedProperty().addListener((obs, was, is) -> {
            if (!is) ValidationHelper.validateMoney(
                    amountField, amountError, "Сумма");
        });

        ComboBox<Donation.PaymentMethod> methodCombo = new ComboBox<>(
                FXCollections.observableArrayList(
                        Donation.PaymentMethod.values()));
        methodCombo.setStyle(StyleManager.textFieldStyle());
        methodCombo.setMaxWidth(Double.MAX_VALUE);
        methodCombo.getSelectionModel().selectFirst();

        TextField commentField = styledField("Комментарий", "");

        CheckBox anonymousCheck = new CheckBox(
                "Анонимное пожертвование");
        anonymousCheck.setStyle("-fx-text-fill: " + DARK + ";");
        anonymousCheck.setOnAction(e -> {
            donorField.setDisable(anonymousCheck.isSelected());
            if (anonymousCheck.isSelected()) {
                donorField.clear();
                donorField.setStyle(StyleManager.textFieldStyle());
                donorError.setVisible(false);
            }
        });

        Label fundError = errorLabel();

        content.getChildren().addAll(
                lbl("Фонд *"), fundCombo, fundError,
                lbl("Проект"), projectCombo,
                anonymousCheck,
                lbl("Имя донора *"), donorField, donorError,
                lbl("Email донора"), emailField, emailError,
                lbl("Сумма (BYN) *"), amountField, amountError,
                lbl("Способ оплаты *"), methodCombo,
                lbl("Комментарий"), commentField
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle(
                "-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean valid = true;
            if (fundCombo.getValue() == null) {
                fundError.setText("Выберите фонд");
                fundError.setVisible(true);
                valid = false;
            } else fundError.setVisible(false);
            if (!anonymousCheck.isSelected()) {
                if (!ValidationHelper.validateRequired(
                        donorField, donorError, "Имя донора"))
                    valid = false;
            }
            if (!ValidationHelper.validateEmail(emailField, emailError))
                valid = false;
            if (!ValidationHelper.validateMoney(
                    amountField, amountError, "Сумма"))
                valid = false;
            if (!valid) ev.consume();
        });

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            Donation d = new Donation();
            d.setFundId(fundCombo.getValue().getId());
            if (projectCombo.getValue() != null)
                d.setProjectId(projectCombo.getValue().getId());
            d.setAnonymous(anonymousCheck.isSelected());
            d.setDonorName(anonymousCheck.isSelected()
                    ? "Анонимно" : donorField.getText().trim());
            d.setDonorEmail(emailField.getText().trim());
            d.setAmount(ValidationHelper.parseMoney(amountField));
            d.setPaymentMethod(methodCombo.getValue());
            d.setComment(commentField.getText().trim());
            return d;
        });

        dialog.showAndWait().ifPresent(d ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Request req = new Request(
                                Request.Action.REGISTER_DONATION, null);
                        req.addParam("donation", d);
                        return ServerConnection.getInstance().send(req);
                    } catch (Exception e) {
                        return Response.error(e.getMessage());
                    }
                }).thenAcceptAsync(response -> {
                    if (response.isSuccess()) {
                        loadData();
                        showThankYouDialog(d.getAmount());
                    } else showAlert("Ошибка", response.getMessage());
                }, Platform::runLater));
    }

    private void showThankYouDialog(BigDecimal amount) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Спасибо!");
        dialog.setResizable(false);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: "
                + StyleManager.PINK_LIGHT + ";");
        content.setMinWidth(400);

        Label heart = new Label("♥");
        heart.setStyle("-fx-font-size: 64px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + PINK + ";");

        Label title = new Label("Спасибо за вашу доброту!");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        Label amtLbl = new Label("Ваше пожертвование: "
                + formatByn(amount));
        amtLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + PINK + ";");

        Label msg = new Label(
                "Каждый вклад меняет чью-то жизнь к лучшему.\n"
                        + "Ваша помощь уже на пути к тем, кто в ней нуждается.\n"
                        + "Вместе мы делаем мир добрее!");
        msg.setStyle("-fx-font-size: 14px; -fx-text-fill: " + DARK + ";");
        msg.setTextAlignment(TextAlignment.CENTER);
        msg.setWrapText(true);
        msg.setMaxWidth(320);

        Button closeBtn = new Button("Закрыть");
        closeBtn.setStyle(StyleManager.buttonPrimary());
        closeBtn.setOnAction(e -> dialog.close());

        content.getChildren().addAll(heart, title, amtLbl, msg, closeBtn);
        dialog.setScene(new Scene(content));
        dialog.showAndWait();
    }

    // --- Helpers ---

    private HBox infoRow(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + GRAY + ";");
        lbl.setMinWidth(100);
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 13px; -fx-text-fill: " + DARK + ";");
        val.setWrapText(true);
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1)
                + parts[1].substring(0, 1)).toUpperCase();
    }

    private <T> TableCell<Donation, T> textCell(String color) {
        return new TableCell<>() {
            @Override protected void updateItem(T val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : val.toString());
                setStyle("-fx-text-fill: " + color + ";");
            }
        };
    }

    private TextField styledField(String prompt, String val) {
        TextField tf = new TextField(val);
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

    private String formatByn(BigDecimal val) {
        if (val == null) return "0,00 Br";
        return String.format("%,.2f Br", val)
                .replace(',', ' ').replace('.', ',');
    }
}