package by.charity.server.repository;

import by.charity.server.db.ConnectionPool;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.Donation;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DonationRepository {

    private static final String SELECT_BASE = """
            SELECT d.*, cf.name as fund_name, p.name as project_name
            FROM donations d
            JOIN charity_funds cf ON d.fund_id = cf.id
            LEFT JOIN projects p ON d.project_id = p.id
            """;

    public List<Donation> findAll() {
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     SELECT_BASE + " ORDER BY d.donated_at DESC")) {
            List<Donation> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка получения пожертвований", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public List<Donation> findByFundId(Long fundId) {
        String sql = SELECT_BASE
                + " WHERE d.fund_id = ? ORDER BY d.donated_at DESC";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fundId);
            ResultSet rs = ps.executeQuery();
            List<Donation> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка получения пожертвований фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public List<Donation> findByProjectId(Long projectId) {
        String sql = SELECT_BASE
                + " WHERE d.project_id = ? ORDER BY d.donated_at DESC";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ResultSet rs = ps.executeQuery();
            List<Donation> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка получения пожертвований проекта", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public Optional<Donation> findById(Long id) {
        String sql = SELECT_BASE + " WHERE d.id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка поиска пожертвования", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public Donation save(Donation d) {
        String sql = """
                INSERT INTO donations (fund_id, project_id, donor_name,
                    donor_email, amount, payment_method, comment,
                    anonymous, donated_at, registered_by_user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, d.getFundId());
            if (d.getProjectId() != null) ps.setLong(2, d.getProjectId());
            else ps.setNull(2, Types.BIGINT);
            ps.setString(3, d.getDonorName());
            ps.setString(4, d.getDonorEmail());
            ps.setBigDecimal(5, d.getAmount());
            ps.setString(6, d.getPaymentMethod().name());
            ps.setString(7, d.getComment());
            ps.setBoolean(8, d.isAnonymous());
            if (d.getRegisteredByUserId() != null)
                ps.setLong(9, d.getRegisteredByUserId());
            else ps.setNull(9, Types.BIGINT);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) d.setId(keys.getLong(1));
            return d;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка сохранения пожертвования", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM donations WHERE id=?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка удаления пожертвования", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void nullifyRegisteredByUserId(Long userId) {
        String sql = "UPDATE donations SET registered_by_user_id = NULL "
                + "WHERE registered_by_user_id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка обнуления ссылки на пользователя в пожертвованиях", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void deleteByFundId(Long fundId) {
        String sql = "DELETE FROM donations WHERE fund_id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fundId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка удаления пожертвований фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public BigDecimal sumByFundId(Long fundId) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM donations WHERE fund_id = ?
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fundId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
            return BigDecimal.ZERO;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка подсчёта пожертвований", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public BigDecimal sumByProjectId(Long projectId) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM donations WHERE project_id = ?
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
            return BigDecimal.ZERO;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка подсчёта пожертвований проекта", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    private Donation mapRow(ResultSet rs) throws SQLException {
        Donation d = new Donation();
        d.setId(rs.getLong("id"));
        d.setFundId(rs.getLong("fund_id"));
        d.setFundName(rs.getString("fund_name"));
        long projectId = rs.getLong("project_id");
        if (!rs.wasNull()) {
            d.setProjectId(projectId);
            d.setProjectName(rs.getString("project_name"));
        }
        d.setDonorName(rs.getString("donor_name"));
        d.setDonorEmail(rs.getString("donor_email"));
        d.setAmount(rs.getBigDecimal("amount"));
        d.setPaymentMethod(Donation.PaymentMethod.valueOf(
                rs.getString("payment_method")));
        d.setComment(rs.getString("comment"));
        d.setAnonymous(rs.getBoolean("anonymous"));
        Timestamp ts = rs.getTimestamp("donated_at");
        if (ts != null) d.setDonatedAt(ts.toLocalDateTime());
        long regBy = rs.getLong("registered_by_user_id");
        if (!rs.wasNull()) d.setRegisteredByUserId(regBy);
        return d;
    }
}