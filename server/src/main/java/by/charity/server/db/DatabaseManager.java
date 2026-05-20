package by.charity.server.db;

import by.charity.shared.exception.CharityException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger logger =
            Logger.getLogger(DatabaseManager.class.getName());

    private static final String[] SCHEMA_STATEMENTS = {
            """
        CREATE TABLE IF NOT EXISTS users (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(100) NOT NULL UNIQUE,
            password_hash VARCHAR(255) NOT NULL,
            full_name VARCHAR(255) NOT NULL,
            email VARCHAR(255),
            description TEXT,
            role VARCHAR(50) NOT NULL DEFAULT 'GUEST',
            active TINYINT(1) NOT NULL DEFAULT 1,
            created_at DATETIME NOT NULL DEFAULT NOW()
        )
        """,
            """
        CREATE TABLE IF NOT EXISTS charity_funds (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            description TEXT,
            registration_number VARCHAR(100) UNIQUE,
            contact_email VARCHAR(255),
            contact_phone VARCHAR(50),
            total_received DECIMAL(15,2) NOT NULL DEFAULT 0.00,
            total_spent DECIMAL(15,2) NOT NULL DEFAULT 0.00,
            active TINYINT(1) NOT NULL DEFAULT 1,
            created_at DATETIME NOT NULL DEFAULT NOW()
        )
        """,
            """
        CREATE TABLE IF NOT EXISTS projects (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            fund_id BIGINT NOT NULL,
            name VARCHAR(255) NOT NULL,
            description TEXT,
            goal_amount DECIMAL(15,2) NOT NULL,
            raised_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
            status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
            start_date DATE,
            end_date DATE,
            created_at DATETIME NOT NULL DEFAULT NOW(),
            FOREIGN KEY (fund_id) REFERENCES charity_funds(id)
                ON DELETE CASCADE
        )
        """,
            """
        CREATE TABLE IF NOT EXISTS donations (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            fund_id BIGINT NOT NULL,
            project_id BIGINT,
            donor_name VARCHAR(255) NOT NULL,
            donor_email VARCHAR(255),
            amount DECIMAL(15,2) NOT NULL,
            payment_method VARCHAR(50) NOT NULL,
            comment TEXT,
            anonymous TINYINT(1) NOT NULL DEFAULT 0,
            donated_at DATETIME NOT NULL DEFAULT NOW(),
            registered_by_user_id BIGINT,
            CONSTRAINT chk_amount CHECK (amount > 0),
            FOREIGN KEY (fund_id) REFERENCES charity_funds(id)
                ON DELETE CASCADE,
            FOREIGN KEY (project_id) REFERENCES projects(id)
                ON DELETE SET NULL,
            FOREIGN KEY (registered_by_user_id) REFERENCES users(id)
                ON DELETE SET NULL
        )
        """,
            """
        CREATE TABLE IF NOT EXISTS reports (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            fund_id BIGINT NOT NULL,
            project_id BIGINT,
            type VARCHAR(50) NOT NULL,
            title VARCHAR(255) NOT NULL,
            period_start DATE NOT NULL,
            period_end DATE NOT NULL,
            total_received DECIMAL(15,2) NOT NULL DEFAULT 0.00,
            total_spent DECIMAL(15,2) NOT NULL DEFAULT 0.00,
            donations_count INT NOT NULL DEFAULT 0,
            notes TEXT,
            is_public TINYINT(1) NOT NULL DEFAULT 0,
            created_by_user_id BIGINT,
            created_at DATETIME NOT NULL DEFAULT NOW(),
            FOREIGN KEY (fund_id) REFERENCES charity_funds(id)
                ON DELETE CASCADE,
            FOREIGN KEY (project_id) REFERENCES projects(id)
                ON DELETE SET NULL,
            FOREIGN KEY (created_by_user_id) REFERENCES users(id)
                ON DELETE SET NULL
        )
        """,
            """
        INSERT IGNORE INTO users (username, password_hash, full_name, role)
        VALUES ('admin',
            '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918',
            'Администратор системы', 'ADMIN')
        """
    };

    public static void initializeSchema() {
        Connection conn = ConnectionPool.getInstance().getConnection();
        try {
            for (String sql : SCHEMA_STATEMENTS) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            // Добавляем новые столбцы если их нет (для существующих БД)
            runSafe(conn, """
                ALTER TABLE users
                ADD COLUMN description TEXT
                """);
            runSafe(conn, """
                ALTER TABLE reports
                ADD COLUMN is_public TINYINT(1) NOT NULL DEFAULT 0
                """);
            logger.info("Схема базы данных успешно инициализирована");
        } catch (SQLException e) {
            logger.severe("Ошибка инициализации схемы БД: " + e.getMessage());
            throw new CharityException.DatabaseException(
                    "Ошибка инициализации схемы", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    private static void runSafe(Connection conn, String sql) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql.trim());
        } catch (SQLException ignored) {
            // Столбец уже существует — игнорируем
        }
    }
}