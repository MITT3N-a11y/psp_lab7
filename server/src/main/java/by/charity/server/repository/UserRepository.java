package by.charity.server.repository;

import by.charity.server.db.ConnectionPool;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.Role;
import by.charity.shared.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка поиска пользователя", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка поиска пользователя", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<User> users = new ArrayList<>();
            while (rs.next()) users.add(mapRow(rs));
            return users;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка получения пользователей", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public User save(User user) {
        String sql = """
                INSERT INTO users (username, password_hash, full_name,
                    email, description, role, active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getDescription());
            ps.setString(6, user.getRole().name());
            ps.setBoolean(7, user.isActive());
            ps.setTimestamp(8, Timestamp.valueOf(
                    user.getCreatedAt() != null
                            ? user.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) user.setId(keys.getLong(1));
            return user;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка сохранения пользователя", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void update(User user) {
        String sql = """
                UPDATE users
                SET full_name=?, email=?, description=?, role=?, active=?
                WHERE id=?
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getDescription());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.setLong(6, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка обновления пользователя", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void updatePassword(Long userId, String newHash) {
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка смены пароля", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id=?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка удаления пользователя", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setDescription(rs.getString("description"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }
}