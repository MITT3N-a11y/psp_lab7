package by.charity.server.db;

import by.charity.server.config.ServerConfig;
import by.charity.shared.exception.CharityException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class ConnectionPool {
    private static final Logger logger = Logger.getLogger(ConnectionPool.class.getName());
    private static ConnectionPool instance;

    private final BlockingQueue<Connection> pool;
    private final String url;
    private final String user;
    private final String password;

    private ConnectionPool() {
        ServerConfig config = ServerConfig.getInstance();
        this.url = config.getDbUrl();
        this.user = config.getDbUser();
        this.password = config.getDbPassword();
        int size = config.getDbPoolSize();

        pool = new ArrayBlockingQueue<>(size);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            for (int i = 0; i < size; i++) {
                pool.offer(createConnection());
            }
            logger.info("Пул соединений создан, размер: " + size);
        } catch (Exception e) {
            throw new CharityException.DatabaseException("Ошибка инициализации пула соединений", e);
        }
    }

    public static synchronized ConnectionPool getInstance() {
        if (instance == null) {
            instance = new ConnectionPool();
        }
        return instance;
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public Connection getConnection() {
        try {
            Connection conn = pool.take();
            if (conn.isClosed() || !conn.isValid(2)) {
                conn = createConnection();
            }
            return conn;
        } catch (Exception e) {
            throw new CharityException.DatabaseException("Ошибка получения соединения из пула", e);
        }
    }

    public void releaseConnection(Connection conn) {
        if (conn != null) {
            pool.offer(conn);
        }
    }

    public void shutdown() {
        for (Connection conn : pool) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
        logger.info("Пул соединений закрыт");
    }
}