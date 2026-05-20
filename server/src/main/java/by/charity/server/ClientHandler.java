package by.charity.server;

import by.charity.server.service.*;
import by.charity.shared.dto.Request;
import by.charity.shared.dto.Response;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger logger =
            Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final UserService userService;
    private final FundService fundService;
    private final ProjectService projectService;
    private final DonationService donationService;
    private final ReportService reportService;

    public ClientHandler(Socket socket, UserService userService,
                         FundService fundService,
                         ProjectService projectService,
                         DonationService donationService,
                         ReportService reportService) {
        this.socket = socket;
        this.userService = userService;
        this.fundService = fundService;
        this.projectService = projectService;
        this.donationService = donationService;
        this.reportService = reportService;
    }

    @Override
    public void run() {
        String addr = socket.getInetAddress().getHostAddress();
        logger.info("Клиент подключился: " + addr);
        try (ObjectInputStream in =
                     new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out =
                     new ObjectOutputStream(socket.getOutputStream())) {
            while (!socket.isClosed()) {
                try {
                    Request request = (Request) in.readObject();
                    Response response = handleRequest(request);
                    out.writeObject(response);
                    out.flush();
                    out.reset();
                } catch (EOFException e) {
                    break;
                } catch (ClassNotFoundException e) {
                    out.writeObject(Response.error(
                            "Неверный формат запроса"));
                    out.flush();
                }
            }
        } catch (IOException e) {
            logger.info("Клиент отключился: " + addr);
        }
    }

    @SuppressWarnings("unchecked")
    private Response handleRequest(Request req) {
        try {
            String token = req.getSessionToken();
            return switch (req.getAction()) {

                // --- Auth ---
                case LOGIN -> {
                    String t = userService.login(
                            (String) req.getParam("username"),
                            (String) req.getParam("password"));
                    User user = userService.getUserByToken(t);
                    yield Response.success(Map.of("token", t, "user", user));
                }
                case LOGOUT -> {
                    userService.logout(token);
                    yield Response.success("Выход выполнен", null);
                }
                case CHANGE_PASSWORD -> {
                    userService.changePassword(token,
                            (Long) req.getParam("userId"),
                            (String) req.getParam("oldPassword"),
                            (String) req.getParam("newPassword"));
                    yield Response.success("Пароль изменён", null);
                }
                case REGISTER_GUEST -> {
                    User created = userService.registerGuest(
                            (User) req.getParam("user"));
                    yield Response.success("Регистрация успешна", created);
                }
                case GET_MY_PROFILE ->
                        Response.success(userService.getUserByToken(token));
                case UPDATE_PROFILE -> {
                    userService.updateProfile(token,
                            (User) req.getParam("user"));
                    yield Response.success("Профиль обновлён", null);
                }

                // --- Users ---
                case GET_ALL_USERS ->
                        Response.success(userService.getAllUsers(token));
                case GET_USER_BY_ID ->
                        Response.success(userService.getUserById(token,
                                (Long) req.getParam("id")));
                case CREATE_USER ->
                        Response.success(userService.createUser(token,
                                (User) req.getParam("user")));
                case UPDATE_USER -> {
                    userService.updateUser(token,
                            (User) req.getParam("user"));
                    yield Response.success("Пользователь обновлён", null);
                }
                case DEACTIVATE_USER -> {
                    userService.deactivateUser(token,
                            (Long) req.getParam("id"));
                    yield Response.success(
                            "Пользователь деактивирован", null);
                }
                case DELETE_USER -> {
                    userService.deleteUser(token,
                            (Long) req.getParam("id"));
                    yield Response.success("Пользователь удалён", null);
                }

                // --- Funds ---
                case GET_ALL_FUNDS ->
                        Response.success(
                                fundService.getAllFunds(token, userService));
                case GET_FUND_BY_ID ->
                        Response.success(fundService.getFundById(token,
                                (Long) req.getParam("id"), userService));
                case CREATE_FUND ->
                        Response.success(fundService.createFund(token,
                                (CharityFund) req.getParam("fund"),
                                userService));
                case UPDATE_FUND -> {
                    fundService.updateFund(token,
                            (CharityFund) req.getParam("fund"), userService);
                    yield Response.success("Фонд обновлён", null);
                }
                case DEACTIVATE_FUND -> {
                    fundService.deactivateFund(token,
                            (Long) req.getParam("id"), userService);
                    yield Response.success("Фонд деактивирован", null);
                }
                case DELETE_FUND -> {
                    fundService.deleteFund(token,
                            (Long) req.getParam("id"), userService);
                    yield Response.success("Фонд удалён", null);
                }
                case ADD_FUND_EXPENSE -> {
                    fundService.addExpense(token,
                            (Long) req.getParam("fundId"),
                            (java.math.BigDecimal) req.getParam("amount"),
                            userService);
                    yield Response.success("Расход добавлен", null);
                }

                // --- Projects ---
                case GET_ALL_PROJECTS ->
                        Response.success(
                                projectService.getAllProjects(
                                        token, userService));
                case GET_PROJECTS_BY_FUND ->
                        Response.success(projectService.getProjectsByFund(
                                token, (Long) req.getParam("fundId"),
                                userService));
                case GET_PROJECT_BY_ID ->
                        Response.success(projectService.getProjectById(
                                token, (Long) req.getParam("id"),
                                userService));
                case CREATE_PROJECT ->
                        Response.success(projectService.createProject(
                                token, (Project) req.getParam("project"),
                                userService));
                case UPDATE_PROJECT -> {
                    projectService.updateProject(token,
                            (Project) req.getParam("project"), userService);
                    yield Response.success("Проект обновлён", null);
                }
                case UPDATE_PROJECT_STATUS -> {
                    projectService.updateProjectStatus(token,
                            (Long) req.getParam("projectId"),
                            (Project.Status) req.getParam("status"),
                            userService);
                    yield Response.success(
                            "Статус проекта обновлён", null);
                }
                case DELETE_PROJECT -> {
                    projectService.deleteProject(token,
                            (Long) req.getParam("id"), userService);
                    yield Response.success("Проект удалён", null);
                }

                // --- Donations ---
                case GET_ALL_DONATIONS ->
                        Response.success(
                                donationService.getAllDonations(
                                        token, userService));
                case GET_DONATIONS_BY_FUND ->
                        Response.success(donationService.getDonationsByFund(
                                token, (Long) req.getParam("fundId"),
                                userService));
                case GET_DONATIONS_BY_PROJECT ->
                        Response.success(
                                donationService.getDonationsByProject(
                                        token,
                                        (Long) req.getParam("projectId"),
                                        userService));
                case REGISTER_DONATION ->
                        Response.success(donationService.registerDonation(
                                token,
                                (Donation) req.getParam("donation"),
                                userService));
                case DELETE_DONATION -> {
                    donationService.deleteDonation(token,
                            (Long) req.getParam("id"), userService);
                    yield Response.success("Пожертвование удалено", null);
                }
                case GET_TOP_DONORS ->
                        Response.success(donationService.getTopDonors(
                                token, userService));

                // --- Reports ---
                case GET_ALL_REPORTS ->
                        Response.success(
                                reportService.getAllReports(
                                        token, userService));
                case GET_PUBLIC_REPORTS ->
                        Response.success(reportService.getPublicReports());
                case GET_REPORT_BY_ID ->
                        Response.success(reportService.getReportById(
                                token, (Long) req.getParam("id"),
                                userService));
                case GENERATE_REPORT ->
                        Response.success(reportService.generateReport(
                                token,
                                (Long) req.getParam("fundId"),
                                (Long) req.getParam("projectId"),
                                (Report.ReportType) req.getParam("type"),
                                (LocalDate) req.getParam("start"),
                                (LocalDate) req.getParam("end"),
                                (String) req.getParam("notes"),
                                Boolean.TRUE.equals(
                                        req.getParam("isPublic")),
                                userService));
                case DELETE_REPORT -> {
                    reportService.deleteReport(token,
                            (Long) req.getParam("id"), userService);
                    yield Response.success("Отчёт удалён", null);
                }
                case GET_FUND_STATISTICS ->
                        Response.success(
                                reportService.getFundStatistics(
                                        token, userService));
                case GET_DONATION_STATISTICS ->
                        Response.success(
                                donationService.getDonationStatistics(
                                        token,
                                        (Long) req.getParam("fundId"),
                                        userService));

                default -> Response.error(
                        "Неизвестное действие: " + req.getAction());
            };
        } catch (CharityException.AuthException e) {
            return Response.unauthorized();
        } catch (CharityException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.severe("Ошибка: " + e.getMessage());
            return Response.error("Внутренняя ошибка сервера");
        }
    }
}