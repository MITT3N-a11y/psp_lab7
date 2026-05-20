package by.charity.server;

import by.charity.server.config.ServerConfig;
import by.charity.server.db.ConnectionPool;
import by.charity.server.db.DatabaseManager;
import by.charity.server.service.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private final ServerConfig config;
    private final UserService userService = new UserService();
    private final FundService fundService = new FundService();
    private final ProjectService projectService = new ProjectService();
    private final DonationService donationService = new DonationService();
    private final ReportService reportService = new ReportService();

    private volatile boolean running = false;

    public Server() {
        this.config = ServerConfig.getInstance();
    }

    public static void main(String[] args) {
        ServerConfig.getInstance().loadFromArgs(args);
        new Server().start();
    }

    public void start() {
        ThreadPoolManager.getInstance().init(config.getThreadPoolSize());
        DatabaseManager.initializeSchema();

        running = true;
        logger.info("Сервер запущен на порту " + config.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        try (ServerSocket serverSocket = new ServerSocket(config.getPort())) {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(
                            clientSocket, userService, fundService,
                            projectService, donationService, reportService);
                    ThreadPoolManager.getInstance().submit(handler);
                } catch (IOException e) {
                    if (running) logger.warning("Ошибка принятия соединения: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.severe("Критическая ошибка сервера: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        ThreadPoolManager.getInstance().shutdown();
        ConnectionPool.getInstance().shutdown();
        logger.info("Сервер остановлен");
    }
}