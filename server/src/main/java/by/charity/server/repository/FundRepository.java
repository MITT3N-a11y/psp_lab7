package by.charity.server.repository;

import by.charity.server.db.ConnectionPool;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.CharityFund;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FundRepository {

    public List<CharityFund> findAll() {
        String sql = "SELECT * FROM charity_funds ORDER BY name";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<CharityFund> funds = new ArrayList<>();
            while (rs.next()) funds.add(mapRow(rs));
            return funds;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка получения фондов", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public Optional<CharityFund> findById(Long id) {
        String sql = "SELECT * FROM charity_funds WHERE id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка поиска фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public CharityFund save(CharityFund fund) {
        String sql = """
                INSERT INTO charity_funds (name, description,
                    registration_number, contact_email, contact_phone,
                    total_received, total_spent, active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fund.getName());
            ps.setString(2, fund.getDescription());
            ps.setString(3, fund.getRegistrationNumber());
            ps.setString(4, fund.getContactEmail());
            ps.setString(5, fund.getContactPhone());
            ps.setBigDecimal(6, fund.getTotalReceived() != null
                    ? fund.getTotalReceived() : BigDecimal.ZERO);
            ps.setBigDecimal(7, fund.getTotalSpent() != null
                    ? fund.getTotalSpent() : BigDecimal.ZERO);
            ps.setBoolean(8, fund.isActive());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) fund.setId(keys.getLong(1));
            return fund;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка сохранения фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void update(CharityFund fund) {
        String sql = """
                UPDATE charity_funds
                SET name=?, description=?, registration_number=?,
                    contact_email=?, contact_phone=?, active=?
                WHERE id=?
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fund.getName());
            ps.setString(2, fund.getDescription());
            ps.setString(3, fund.getRegistrationNumber());
            ps.setString(4, fund.getContactEmail());
            ps.setString(5, fund.getContactPhone());
            ps.setBoolean(6, fund.isActive());
            ps.setLong(7, fund.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка обновления фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void updateTotals(Long fundId, BigDecimal received,
                             BigDecimal spent) {
        String sql = """
                UPDATE charity_funds
                SET total_received=?, total_spent=? WHERE id=?
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, received);
            ps.setBigDecimal(2, spent);
            ps.setLong(3, fundId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка обновления итогов фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void addExpense(Long fundId, BigDecimal amount) {
        String sql = """
            UPDATE charity_funds
            SET total_spent = total_spent + ?
            WHERE id = ?
            """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, fundId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка добавления расхода фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM charity_funds WHERE id=?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка удаления фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    private CharityFund mapRow(ResultSet rs) throws SQLException {
        CharityFund f = new CharityFund();
        f.setId(rs.getLong("id"));
        f.setName(rs.getString("name"));
        f.setDescription(rs.getString("description"));
        f.setRegistrationNumber(rs.getString("registration_number"));
        f.setContactEmail(rs.getString("contact_email"));
        f.setContactPhone(rs.getString("contact_phone"));
        f.setTotalReceived(rs.getBigDecimal("total_received"));
        f.setTotalSpent(rs.getBigDecimal("total_spent"));
        f.setActive(rs.getBoolean("active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) f.setCreatedAt(ts.toLocalDateTime());
        return f;
    }
}