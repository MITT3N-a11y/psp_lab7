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

public class ProjectController {
    private final User currentUser;
    private final ObservableList<Project> projects =
            FXCollections.observableArrayList();
    private List<CharityFund> fundList;

    private static final String DARK = StyleManager.TEXT_DARK;
    private static final String GRAY = StyleManager.TEXT_GRAY;

    public ProjectController(User currentUser) {
        this.currentUser = currentUser;
    }

    public javafx.scene.Node createView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("Проекты");
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Кнопка добавления — только ADMIN и MANAGER
        boolean canEdit = currentUser.getRole() == Role.ADMIN
                || currentUser.getRole() == Role.MANAGER;

        Button addBtn = new Button("+ Новый проект");
        addBtn.setStyle(StyleManager.buttonPrimary());
        addBtn.setVisible(canEdit);
        addBtn.setManaged(canEdit);

        headerRow.getChildren().addAll(header, spacer, addBtn);

        // Подсказка для гостя
        if (currentUser.getRole() == Role.GUEST) {
            Label hint = new Label(
                    "Вы просматриваете проекты в режиме чтения.");
            hint.setStyle("-fx-font-size: 12px; -fx-text-fill: " + GRAY + ";");
            root.getChildren().add(hint);
        }

        TableView<Project> table = buildTable(canEdit);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(headerRow, table);

        addBtn.setOnAction(e -> showProjectDialog(null));
        loadData();
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<Project> buildTable(boolean canEdit) {
        TableView<Project> tv = new TableView<>(projects);
        tv.setStyle(StyleManager.tableStyle());
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Project, String> nameCol = new TableColumn<>("Название");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(col -> textCell(DARK, false));

        TableColumn<Project, String> fundCol = new TableColumn<>("Фонд");
        fundCol.setCellValueFactory(new PropertyValueFactory<>("fundName"));
        fundCol.setCellFactory(col -> textCell(GRAY, false));

        TableColumn<Project, BigDecimal> goalCol =
                new TableColumn<>("Цель (BYN)");
        goalCol.setCellValueFactory(
                new PropertyValueFactory<>("goalAmount"));
        goalCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : formatByn(v));
                setStyle("-fx-text-fill: " + DARK + ";");
            }
        });

        TableColumn<Project, BigDecimal> raisedCol =
                new TableColumn<>("Собрано (BYN)");
        raisedCol.setCellValueFactory(
                new PropertyValueFactory<>("raisedAmount"));
        raisedCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : formatByn(v));
                setStyle("-fx-text-fill: " + StyleManager.SUCCESS + ";"
                        + " -fx-font-weight: bold;");
            }
        });

        TableColumn<Project, Void> progressCol =
                new TableColumn<>("Прогресс");
        progressCol.setPrefWidth(140);
        progressCol.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar pb = new ProgressBar(0);
            {
                pb.setMaxWidth(Double.MAX_VALUE);
                pb.setStyle("-fx-accent: " + StyleManager.PINK_DEEP + ";");
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                Project p = getTableView().getItems().get(getIndex());
                pb.setProgress(Math.min(p.getProgressPercent() / 100.0, 1.0));
                setGraphic(pb);
            }
        });

        TableColumn<Project, Project.Status> statusCol =
                new TableColumn<>("Статус");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(
                    Project.Status v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v.getDisplayName());
                String color = switch (v) {
                    case ACTIVE    -> StyleManager.SUCCESS;
                    case COMPLETED -> StyleManager.PINK_DEEP;
                    case SUSPENDED -> StyleManager.DANGER;
                    default        -> GRAY;
                };
                setStyle("-fx-text-fill: " + color + ";");
            }
        });
        statusCol.setPrefWidth(110);

        // Колонка действий — только для ADMIN и MANAGER
        if (canEdit) {
            TableColumn<Project, Void> actionsCol =
                    new TableColumn<>("Действия");
            actionsCol.setPrefWidth(200);
            actionsCol.setCellFactory(col -> new TableCell<>() {
                private final Button editBtn  = new Button("Изменить");
                private final Button deleteBtn = new Button("Удалить");
                private final HBox box =
                        new HBox(6, editBtn, deleteBtn);
                {
                    editBtn.setStyle(StyleManager.buttonSecondary());
                    deleteBtn.setStyle(StyleManager.buttonDanger());
                    box.setAlignment(Pos.CENTER);
                    editBtn.setOnAction(e ->
                            showProjectDialog(
                                    getTableView().getItems()
                                            .get(getIndex())));
                    deleteBtn.setOnAction(e ->
                            deleteProject(
                                    getTableView().getItems()
                                            .get(getIndex())));
                    // Удаление — только ADMIN
                    deleteBtn.setVisible(
                            currentUser.getRole() == Role.ADMIN);
                    deleteBtn.setManaged(
                            currentUser.getRole() == Role.ADMIN);
                }
                @Override protected void updateItem(Void v, boolean empty) {
                    super.updateItem(v, empty);
                    setGraphic(empty ? null : box);
                }
            });
            tv.getColumns().addAll(nameCol, fundCol, goalCol, raisedCol,
                    progressCol, statusCol, actionsCol);
        } else {
            // Гость и бухгалтер — только просмотр, без кнопок
            tv.getColumns().addAll(nameCol, fundCol, goalCol, raisedCol,
                    progressCol, statusCol);
        }
        return tv;
    }

    private void loadData() {
        CompletableFuture.supplyAsync(() -> {
            try {
                Response r1 = ServerConnection.getInstance().send(
                        new Request(Request.Action.GET_ALL_PROJECTS, null));
                Response r2 = ServerConnection.getInstance().send(
                        new Request(Request.Action.GET_ALL_FUNDS, null));
                return new Response[]{r1, r2};
            } catch (Exception e) { return null; }
        }).thenAcceptAsync(responses -> {
            if (responses == null) return;
            if (responses[0].isSuccess())
                projects.setAll((List<Project>) responses[0].getData());
            if (responses[1].isSuccess())
                fundList = (List<CharityFund>) responses[1].getData();
        }, Platform::runLater);
    }

    private void deleteProject(Project project) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Полностью удалить проект «" + project.getName()
                        + "» из базы данных?\n"
                        + "Все связанные данные также будут удалены.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            CompletableFuture.supplyAsync(() -> {
                try {
                    Request req = new Request(
                            Request.Action.DELETE_PROJECT, null);
                    req.addParam("id", project.getId());
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

    private void showProjectDialog(Project existing) {
        if (fundList == null || fundList.isEmpty()) {
            showAlert("Нет фондов",
                    "Сначала создайте хотя бы один фонд.");
            return;
        }
        boolean isNew = existing == null;
        Project project = isNew ? new Project() : existing;

        Dialog<Project> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Новый проект" : "Редактировать проект");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setMinWidth(400);

        ComboBox<CharityFund> fundCombo = new ComboBox<>(
                FXCollections.observableArrayList(fundList));
        fundCombo.setStyle(StyleManager.textFieldStyle());
        fundCombo.setMaxWidth(Double.MAX_VALUE);
        if (!isNew)
            fundList.stream()
                    .filter(f -> f.getId().equals(project.getFundId()))
                    .findFirst()
                    .ifPresent(fundCombo::setValue);

        TextField nameField = styledField("Название *", project.getName());
        TextField descField = styledField("Описание", project.getDescription());
        TextField goalField = styledField("Целевая сумма (BYN) *",
                project.getGoalAmount() != null
                        ? project.getGoalAmount().toPlainString() : "");

        DatePicker startPicker = new DatePicker(
                project.getStartDate() != null
                        ? project.getStartDate() : LocalDate.now());
        DatePicker endPicker = new DatePicker(
                project.getEndDate() != null
                        ? project.getEndDate()
                        : LocalDate.now().plusMonths(3));
        startPicker.setStyle(StyleManager.textFieldStyle());
        endPicker.setStyle(StyleManager.textFieldStyle());

        ComboBox<Project.Status> statusCombo = new ComboBox<>(
                FXCollections.observableArrayList(Project.Status.values()));
        statusCombo.setStyle(StyleManager.textFieldStyle());
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.setValue(project.getStatus() != null
                ? project.getStatus() : Project.Status.PLANNED);

        content.getChildren().addAll(
                lbl("Фонд *"), fundCombo,
                lbl("Название *"), nameField,
                lbl("Описание"), descField,
                lbl("Целевая сумма (BYN) *"), goalField,
                lbl("Дата начала"), startPicker,
                lbl("Дата окончания"), endPicker,
                lbl("Статус"), statusCombo
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle(
                "-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            if (fundCombo.getValue() == null) {
                showAlert("Ошибка", "Выберите фонд");
                return null;
            }
            project.setFundId(fundCombo.getValue().getId());
            project.setName(nameField.getText().trim());
            project.setDescription(descField.getText().trim());
            try {
                project.setGoalAmount(new BigDecimal(
                        goalField.getText().trim().replace(',', '.')));
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Неверный формат суммы");
                return null;
            }
            project.setStartDate(startPicker.getValue());
            project.setEndDate(endPicker.getValue());
            project.setStatus(statusCombo.getValue());
            return project;
        });

        dialog.showAndWait().ifPresent(p ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Request req = isNew
                                ? new Request(
                                Request.Action.CREATE_PROJECT, null)
                                : new Request(
                                Request.Action.UPDATE_PROJECT, null);
                        req.addParam("project", p);
                        return ServerConnection.getInstance().send(req);
                    } catch (Exception e) {
                        return Response.error(e.getMessage());
                    }
                }).thenAcceptAsync(response -> {
                    if (response.isSuccess()) loadData();
                    else showAlert("Ошибка", response.getMessage());
                }, Platform::runLater));
    }

    private <T> TableCell<Project, T> textCell(String color, boolean bold) {
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

    private String formatByn(BigDecimal val) {
        if (val == null) return "0,00 Br";
        return String.format("%,.2f Br", val)
                .replace(',', ' ').replace('.', ',');
    }
}