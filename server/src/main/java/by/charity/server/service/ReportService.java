package by.charity.server.service;

import by.charity.server.repository.DonationRepository;
import by.charity.server.repository.FundRepository;
import by.charity.server.repository.ReportRepository;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private final ReportRepository reportRepository = new ReportRepository();
    private final DonationRepository donationRepository = new DonationRepository();
    private final FundRepository fundRepository = new FundRepository();

    public List<Report> getAllReports(String token, UserService userService) {
        User user = userService.getUserByToken(token);
        if (user.getRole() == Role.GUEST) {
            return reportRepository.findAllPublic();
        }
        return reportRepository.findAll();
    }

    public List<Report> getPublicReports() {
        return reportRepository.findAllPublic();
    }

    public Report getReportById(String token, Long id, UserService userService) {
        userService.getUserByToken(token);
        return reportRepository.findById(id)
                .orElseThrow(() -> new CharityException.NotFoundException("Отчёт", id));
    }

    public Report generateReport(String token, Long fundId, Long projectId,
                                 Report.ReportType type, LocalDate start, LocalDate end,
                                 String notes, boolean isPublic, UserService userService) {
        userService.requireRole(token, Role.ADMIN, Role.MANAGER, Role.ACCOUNTANT);

        CharityFund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new CharityException.NotFoundException("Фонд", fundId));

        List<Donation> donations = projectId != null
                ? donationRepository.findByProjectId(projectId)
                : donationRepository.findByFundId(fundId);

        List<Donation> filtered = donations.stream()
                .filter(d -> d.getDonatedAt() != null)
                .filter(d -> {
                    LocalDate date = d.getDonatedAt().toLocalDate();
                    return !date.isBefore(start) && !date.isAfter(end);
                })
                .toList();

        BigDecimal totalReceived = filtered.stream()
                .map(Donation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        User caller = userService.getUserByToken(token);

        Report report = new Report();
        report.setFundId(fundId);
        report.setFundName(fund.getName());
        report.setProjectId(projectId);
        report.setType(type);
        report.setTitle(type.getDisplayName() + " отчёт: " + fund.getName()
                + " (" + start + " — " + end + ")");
        report.setPeriodStart(start);
        report.setPeriodEnd(end);
        report.setTotalReceived(totalReceived);
        report.setTotalSpent(fund.getTotalSpent());
        report.setDonationsCount(filtered.size());
        report.setNotes(notes);
        report.setPublic(isPublic);
        report.setCreatedByUserId(caller.getId());

        return reportRepository.save(report);
    }

    public Map<String, Object> getFundStatistics(String token, UserService userService) {
        userService.getUserByToken(token);
        List<CharityFund> funds = fundRepository.findAll();
        Map<String, Object> stats = new HashMap<>();

        BigDecimal totalReceived = funds.stream()
                .map(CharityFund::getTotalReceived)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = funds.stream()
                .map(CharityFund::getTotalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalFunds", funds.size());
        stats.put("activeFunds", funds.stream().filter(CharityFund::isActive).count());
        stats.put("totalReceived", totalReceived);
        stats.put("totalSpent", totalSpent);
        stats.put("balance", totalReceived.subtract(totalSpent));
        stats.put("totalDonations", donationRepository.findAll().size());

        return stats;
    }

    public void deleteReport(String token, Long id, UserService userService) {
        userService.requireRole(token, Role.ADMIN, Role.MANAGER);
        reportRepository.findById(id)
                .orElseThrow(() ->
                        new CharityException.NotFoundException("Отчёт", id));
        // Простое удаление через SQL
        by.charity.server.db.ConnectionPool conn_pool =
                by.charity.server.db.ConnectionPool.getInstance();
        java.sql.Connection conn = conn_pool.getConnection();
        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM reports WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (java.sql.SQLException e) {
            throw new CharityException.DatabaseException(
                    "Ошибка удаления отчёта", e);
        } finally {
            conn_pool.releaseConnection(conn);
        }
    }
}