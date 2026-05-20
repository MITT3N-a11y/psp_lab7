package by.charity.server.repository;

import by.charity.server.db.ConnectionPool;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.Project;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectRepository {

    private static final String SELECT_BASE = """
            SELECT p.*, cf.name as fund_name
            FROM projects p
            JOIN charity_funds cf ON p.fund_id = cf.id
            """;

    public List<Project> findAll() {
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     SELECT_BASE + " ORDER BY p.created_at DESC")) {
            List<Project> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка получения проектов", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public List<Project> findByFundId(Long fundId) {
        String sql = SELECT_BASE
                + " WHERE p.fund_id = ? ORDER BY p.created_at DESC";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fundId);
            ResultSet rs = ps.executeQuery();
            List<Project> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка получения проектов фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public Optional<Project> findById(Long id) {
        String sql = SELECT_BASE + " WHERE p.id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка поиска проекта", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public Project save(Project project) {
        String sql = """
                INSERT INTO projects (fund_id, name, description,
                    goal_amount, raised_amount, status,
                    start_date, end_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, project.getFundId());
            ps.setString(2, project.getName());
            ps.setString(3, project.getDescription());
            ps.setBigDecimal(4, project.getGoalAmount());
            ps.setBigDecimal(5, project.getRaisedAmount() != null
                    ? project.getRaisedAmount() : BigDecimal.ZERO);
            ps.setString(6, project.getStatus().name());
            ps.setDate(7, project.getStartDate() != null
                    ? Date.valueOf(project.getStartDate()) : null);
            ps.setDate(8, project.getEndDate() != null
                    ? Date.valueOf(project.getEndDate()) : null);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) project.setId(keys.getLong(1));
            return project;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка сохранения проекта", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void update(Project project) {
        String sql = """
                UPDATE projects
                SET name=?, description=?, goal_amount=?,
                    status=?, start_date=?, end_date=?
                WHERE id=?
                """;
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setBigDecimal(3, project.getGoalAmount());
            ps.setString(4, project.getStatus().name());
            ps.setDate(5, project.getStartDate() != null
                    ? Date.valueOf(project.getStartDate()) : null);
            ps.setDate(6, project.getEndDate() != null
                    ? Date.valueOf(project.getEndDate()) : null);
            ps.setLong(7, project.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка обновления проекта", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void updateRaised(Long projectId, BigDecimal raised) {
        String sql = "UPDATE projects SET raised_amount=? WHERE id=?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, raised);
            ps.setLong(2, projectId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка обновления суммы проекта", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM projects WHERE id=?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка удаления проекта", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    public void deleteByFundId(Long fundId) {
        String sql = "DELETE FROM projects WHERE fund_id = ?";
        Connection conn = ConnectionPool.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fundId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CharityException.DatabaseException(
                    "Ошибка удаления проектов фонда", e);
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    private Project mapRow(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setId(rs.getLong("id"));
        p.setFundId(rs.getLong("fund_id"));
        p.setFundName(rs.getString("fund_name"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setGoalAmount(rs.getBigDecimal("goal_amount"));
        p.setRaisedAmount(rs.getBigDecimal("raised_amount"));
        p.setStatus(Project.Status.valueOf(rs.getString("status")));
        Date sd = rs.getDate("start_date");
        if (sd != null) p.setStartDate(sd.toLocalDate());
        Date ed = rs.getDate("end_date");
        if (ed != null) p.setEndDate(ed.toLocalDate());
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
        return p;
    }
}