package by.charity.client.controller;

import by.charity.client.network.ServerConnection;
import by.charity.client.ui.StyleManager;
import by.charity.shared.dto.Request;
import by.charity.shared.dto.Response;
import by.charity.shared.model.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.text.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DashboardController {
    private final User currentUser;

    private static final String DARK  = StyleManager.TEXT_DARK;
    private static final String GRAY  = StyleManager.TEXT_GRAY;
    private static final String PINK  = StyleManager.PINK_DEEP;
    private static final String GREEN = StyleManager.SUCCESS;

    public DashboardController(User currentUser) {
        this.currentUser = currentUser;
    }

    public javafx.scene.Node createView() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + StyleManager.PINK_LIGHT + ";");

        Label header = new Label("Дашборд");
        header.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        VBox welcomeCard = new VBox(6);
        welcomeCard.setPadding(new Insets(16));
        welcomeCard.setStyle(
                "-fx-background-color: white;"
                        + "-fx-border-color: " + StyleManager.PINK_MAIN + ";"
                        + "-fx-border-radius: 12; -fx-background-radius: 12;"
                        + "-fx-border-width: 1.5;");

        Label welcome = new Label(
                "Добро пожаловать, " + currentUser.getFullName() + "!");
        welcome.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        Label roleInfo = new Label(
                "Роль: " + currentUser.getRole().getDisplayName());
        roleInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: " + DARK + ";");

        Label hint = new Label(getHintForRole());
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: " + DARK + ";");
        hint.setWrapText(true);

        welcomeCard.getChildren().addAll(welcome, roleInfo, hint);

        Label statsTitle = new Label("Общая статистика");
        statsTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        VBox activeFundsCard   = makeStatCard("Активных фондов",    "...", StyleManager.PINK_MAIN);
        VBox receivedCard      = makeStatCard("Всего собрано (BYN)", "...", GREEN);
        VBox spentCard         = makeStatCard("Израсходовано (BYN)", "...", StyleManager.WARNING);
        VBox donationsCard     = makeStatCard("Пожертвований",       "...", PINK);
        VBox balanceCard       = makeStatCard("Баланс (BYN)",        "...", StyleManager.PINK_DARK);

        statsRow.getChildren().addAll(
                activeFundsCard, receivedCard, spentCard,
                donationsCard, balanceCard);

        Label recentTitle = new Label("Последние пожертвования");
        recentTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        VBox recentCard = new VBox(0);
        recentCard.setPadding(new Insets(16));
        recentCard.setStyle(
                "-fx-background-color: white;"
                        + "-fx-border-color: " + StyleManager.PINK_MAIN + ";"
                        + "-fx-border-radius: 12; -fx-background-radius: 12;"
                        + "-fx-border-width: 1.5;");

        Label recentLoading = new Label("Загрузка данных...");
        recentLoading.setStyle("-fx-text-fill: " + GRAY + "; -fx-font-size: 13px;");
        recentCard.getChildren().add(recentLoading);

        Label projectsTitle = new Label("Прогресс активных проектов");
        projectsTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + DARK + ";");

        VBox projectsCard = new VBox(0);
        projectsCard.setPadding(new Insets(16));
        projectsCard.setStyle(
                "-fx-background-color: white;"
                        + "-fx-border-color: " + StyleManager.PINK_MAIN + ";"
                        + "-fx-border-radius: 12; -fx-background-radius: 12;"
                        + "-fx-border-width: 1.5;");

        Label projectsLoading = new Label("Загрузка данных...");
        projectsLoading.setStyle("-fx-text-fill: " + GRAY + "; -fx-font-size: 13px;");
        projectsCard.getChildren().add(projectsLoading);

        root.getChildren().addAll(
                header, welcomeCard,
                statsTitle, statsRow,
                recentTitle, recentCard,
                projectsTitle, projectsCard);

        CompletableFuture.supplyAsync(() -> {
            try {
                Response stats = ServerConnection.getInstance().send(
                        new Request(Request.Action.GET_FUND_STATISTICS, null));
                Response donations = ServerConnection.getInstance().send(
                        new Request(Request.Action.GET_ALL_DONATIONS, null));
                Response projects = ServerConnection.getInstance().send(
                        new Request(Request.Action.GET_ALL_PROJECTS, null));
                return new Response[]{stats, donations, projects};
            } catch (Exception e) {
                return null;
            }
        }).thenAcceptAsync(responses -> {
            if (responses == null) return;

            if (responses[0].isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> s = (Map<String, Object>) responses[0].getData();
                setStatValue(activeFundsCard,
                        String.valueOf(s.get("activeFunds")));
                setStatValue(receivedCard,
                        formatByn((BigDecimal) s.get("totalReceived")));
                setStatValue(spentCard,
                        formatByn((BigDecimal) s.get("totalSpent")));
                setStatValue(donationsCard,
                        String.valueOf(s.get("totalDonations")));
                setStatValue(balanceCard,
                        formatByn((BigDecimal) s.get("balance")));
            }

            if (responses[1].isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Donation> all = (List<Donation>) responses[1].getData();
                recentCard.getChildren().clear();

                List<Donation> recent = all.stream().limit(5).toList();

                if (recent.isEmpty()) {
                    Label empty = new Label("Пожертвований пока нет");
                    empty.setStyle("-fx-text-fill: " + GRAY
                            + "; -fx-font-size: 13px;");
                    recentCard.getChildren().add(empty);
                } else {
                    HBox tableHeader = new HBox();
                    tableHeader.setPadding(new Insets(0, 0, 8, 0));
                    Label h1 = colHeader("Сумма (BYN)", 140);
                    Label h2 = colHeader("Донор", 180);
                    Label h3 = colHeader("Фонд", 200);
                    Label h4 = colHeader("Дата", 110);
                    tableHeader.getChildren().addAll(h1, h2, h3, h4);
                    recentCard.getChildren().add(tableHeader);
                    recentCard.getChildren().add(new Separator());

                    DateTimeFormatter fmt =
                            DateTimeFormatter.ofPattern("dd.MM.yyyy");

                    for (Donation d : recent) {
                        HBox row = new HBox();
                        row.setPadding(new Insets(8, 0, 8, 0));
                        row.setAlignment(Pos.CENTER_LEFT);

                        Label amount = colText(
                                formatByn(d.getAmount()), 140,
                                PINK, true);
                        Label donor = colText(
                                d.isAnonymous() ? "Анонимно" : d.getDonorName(),
                                180, DARK, false);
                        Label fund = colText(
                                d.getFundName() != null ? d.getFundName() : "—",
                                200, DARK, false);
                        Label date = colText(
                                d.getDonatedAt() != null
                                        ? d.getDonatedAt().format(fmt) : "—",
                                110, GRAY, false);

                        row.getChildren().addAll(amount, donor, fund, date);
                        recentCard.getChildren().add(row);
                        recentCard.getChildren().add(new Separator());
                    }
                }
            }

            if (responses[2].isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Project> all = (List<Project>) responses[2].getData();
                projectsCard.getChildren().clear();

                List<Project> active = all.stream()
                        .filter(p -> p.getStatus() == Project.Status.ACTIVE)
                        .limit(4)
                        .toList();

                if (active.isEmpty()) {
                    Label empty = new Label("Нет активных проектов");
                    empty.setStyle("-fx-text-fill: " + GRAY
                            + "; -fx-font-size: 13px;");
                    projectsCard.getChildren().add(empty);
                } else {
                    for (int i = 0; i < active.size(); i++) {
                        Project p = active.get(i);

                        VBox row = new VBox(6);
                        row.setPadding(new Insets(8, 0, 8, 0));

                        HBox topLine = new HBox();
                        topLine.setAlignment(Pos.CENTER_LEFT);

                        Label name = new Label(p.getName());
                        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;"
                                + " -fx-text-fill: " + DARK + ";");

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        double pct = Math.min(p.getProgressPercent(), 100.0);
                        Label progress = new Label(
                                String.format("%.1f%%  |  ", pct)
                                        + formatByn(p.getRaisedAmount())
                                        + " из " + formatByn(p.getGoalAmount()));
                        progress.setStyle("-fx-font-size: 12px;"
                                + " -fx-text-fill: " + DARK + ";");

                        topLine.getChildren().addAll(name, spacer, progress);

                        ProgressBar pb = new ProgressBar(pct / 100.0);
                        pb.setMaxWidth(Double.MAX_VALUE);
                        pb.setPrefHeight(10);
                        pb.setStyle("-fx-accent: " + PINK + ";");

                        Label fundName = new Label(
                                "Фонд: " + (p.getFundName() != null
                                        ? p.getFundName() : "—"));
                        fundName.setStyle("-fx-font-size: 11px;"
                                + " -fx-text-fill: " + GRAY + ";");

                        row.getChildren().addAll(topLine, pb, fundName);
                        projectsCard.getChildren().add(row);

                        if (i < active.size() - 1) {
                            projectsCard.getChildren().add(new Separator());
                        }
                    }
                }
            }

        }, Platform::runLater);

        return root;
    }

    // Вспомогательные методы

    private VBox makeStatCard(String title, String value, String accentColor) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(210);
        card.setStyle(
                "-fx-background-color: white;"
                        + "-fx-border-color: " + StyleManager.PINK_MAIN + ";"
                        + "-fx-border-radius: 12; -fx-background-radius: 12;"
                        + "-fx-border-width: 1.5;"
                        + "-fx-padding: 0 0 12 0;");

        Region stripe = new Region();
        stripe.setMinHeight(5);
        stripe.setMaxWidth(Double.MAX_VALUE);
        stripe.setStyle("-fx-background-color: " + accentColor + ";"
                + "-fx-background-radius: 10 10 0 0;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + DARK + ";"
                + "-fx-padding: 0 12 0 12;");

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + accentColor + ";"
                + " -fx-padding: 0 12 0 12;");
        valueLbl.setId("stat-value");

        card.getChildren().addAll(stripe, titleLbl, valueLbl);
        return card;
    }

    private void setStatValue(VBox card, String value) {
        card.getChildren().stream()
                .filter(n -> "stat-value".equals(n.getId()))
                .findFirst()
                .ifPresent(n -> ((Label) n).setText(value));
    }

    private Label colHeader(String text, double width) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;"
                + " -fx-text-fill: " + GRAY + ";");
        l.setMinWidth(width);
        l.setMaxWidth(width);
        return l;
    }

    private Label colText(String text, double width,
                          String color, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px;"
                + (bold ? " -fx-font-weight: bold;" : "")
                + " -fx-text-fill: " + color + ";");
        l.setMinWidth(width);
        l.setMaxWidth(width);
        return l;
    }

    private String getHintForRole() {
        return switch (currentUser.getRole()) {
            case ADMIN ->
                    "Вам доступны все функции: фонды, проекты, "
                            + "пожертвования, отчёты и управление пользователями.";
            case MANAGER ->
                    "Вы можете управлять фондами, проектами, "
                            + "регистрировать пожертвования и формировать отчёты.";
            case ACCOUNTANT ->
                    "Вы можете регистрировать пожертвования "
                            + "и формировать финансовые отчёты.";
            case GUEST ->
                    "Вы можете просматривать фонды, проекты, "
                            + "делать пожертвования и читать публичные отчёты.";
        };
    }

    private String formatByn(BigDecimal val) {
        if (val == null) return "0,00 Br";
        return String.format("%,.2f Br", val)
                .replace(',', ' ').replace('.', ',');
    }
}