package by.charity.server.repository;

import by.charity.server.db.ConnectionPool;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.Report;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportRepository {

    private static final String SELECT_BASE = """
            SELECT r.*, cf.name as fund_name, p.name as project_name
            FROM reports r
            JOIN charity_funds cf ON r.fund_id = cf.id
            LEFT JOIN projects p ON r.project_id = p.id
            """;

    public List<Report> findAll() {
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_BASE + " ORDER BY r.created_at DESC")) {
            List<Report> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException("Ошибка получения отчётов", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public List<Report> findAllPublic() {
        String sql = SELECT_BASE + " WHERE r.is_public = 1 ORDER BY r.created_at DESC";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            List<Report> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException("Ошибка получения публичных отчётов", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public Optional<Report> findById(Long id) {
        String sql = SELECT_BASE + " WHERE r.id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException("Ошибка поиска отчёта", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public Report save(Report report) {
        String sql = """
                INSERT INTO reports (fund_id, project_id, type, title, period_start, period_end,
                    total_received, total_spent, donations_count, notes, is_public,
                    created_by_user_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, report.getFundId());
            if (report.getProjectId() != null) ps.setLong(2, report.getProjectId());
            else ps.setNull(2, Types.BIGINT);
            ps.setString(3, report.getType().name());
            ps.setString(4, report.getTitle());
            ps.setDate(5, Date.valueOf(report.getPeriodStart()));
            ps.setDate(6, Date.valueOf(report.getPeriodEnd()));
            ps.setBigDecimal(7, report.getTotalReceived());
            ps.setBigDecimal(8, report.getTotalSpent());
            ps.setInt(9, report.getDonationsCount());
            ps.setString(10, report.getNotes());
            ps.setBoolean(11, report.isPublic());
            if (report.getCreatedByUserId() != null) ps.setLong(12, report.getCreatedByUserId());
            else ps.setNull(12, Types.BIGINT);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) report.setId(keys.getLong(1));
            return report;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException("Ошибка сохранения отчёта", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void deleteByFundId(Long fundId) {
        String sql = "DELETE FROM reports WHERE fund_id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fundId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка удаления отчётов фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    private Report mapRow(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.setId(rs.getLong("id"));
        r.setFundId(rs.getLong("fund_id"));
        r.setFundName(rs.getString("fund_name"));
        long projectId = rs.getLong("project_id");
        if (!rs.wasNull()) {
            r.setProjectId(projectId);
            r.setProjectName(rs.getString("project_name"));
        }
        r.setType(Report.ReportType.valueOf(rs.getString("type")));
        r.setTitle(rs.getString("title"));
        Date sd = rs.getDate("period_start");
        if (sd != null) r.setPeriodStart(sd.toLocalDate());
        Date ed = rs.getDate("period_end");
        if (ed != null) r.setPeriodEnd(ed.toLocalDate());
        r.setTotalReceived(rs.getBigDecimal("total_received"));
        r.setTotalSpent(rs.getBigDecimal("total_spent"));
        r.setDonationsCount(rs.getInt("donations_count"));
        r.setNotes(rs.getString("notes"));
        r.setPublic(rs.getBoolean("is_public"));
        long createdBy = rs.getLong("created_by_user_id");
        if (!rs.wasNull()) r.setCreatedByUserId(createdBy);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        return r;
    }

    public void nullifyCreatedByUserId(Long userId) {
        String sql = "UPDATE reports SET created_by_user_id = NULL "
                + "WHERE created_by_user_id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка обнуления ссылки на пользователя в отчётах", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }
}