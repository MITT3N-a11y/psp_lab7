package by.charity.server.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class ServerConfig {
    private static final Logger logger = Logger.getLogger(ServerConfig.class.getName());
    private static ServerConfig instance;

    private int port = 8080;
    private int threadPoolSize = 10;
    private String dbUrl = "jdbc:mysql://localhost:3306/charity_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Minsk";
    private String dbUser = "root";
    private String dbPassword = "1234";
    private int dbPoolSize = 5;

    private ServerConfig() {}

    // Singleton pattern
    public static synchronized ServerConfig getInstance() {
        if (instance == null) {
            instance = new ServerConfig();
        }
        return instance;
    }

    public void loadFromFile(String configPath) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)) {
            props.load(fis);
            port = Integer.parseInt(props.getProperty("server.port", String.valueOf(port)));
            threadPoolSize = Integer.parseInt(props.getProperty("server.threads", String.valueOf(threadPoolSize)));
            dbUrl = props.getProperty("db.url", dbUrl);
            dbUser = props.getProperty("db.user", dbUser);
            dbPassword = props.getProperty("db.password", dbPassword);
            dbPoolSize = Integer.parseInt(props.getProperty("db.pool.size", String.valueOf(dbPoolSize)));
            logger.info("Конфигурация загружена из: " + configPath);
        } catch (IOException e) {
            logger.warning("Файл конфигурации не найден, используются значения по умолчанию");
        }
    }

    public void loadFromArgs(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--port" -> port = Integer.parseInt(args[i + 1]);
                case "--threads" -> threadPoolSize = Integer.parseInt(args[i + 1]);
                case "--db-url" -> dbUrl = args[i + 1];
                case "--db-user" -> dbUser = args[i + 1];
                case "--db-password" -> dbPassword = args[i + 1];
                case "--config" -> loadFromFile(args[i + 1]);
            }
        }
    }

    // Getters
    public int getPort() { return port; }
    public int getThreadPoolSize() { return threadPoolSize; }
    public String getDbUrl() { return dbUrl; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public int getDbPoolSize() { return dbPoolSize; }
}