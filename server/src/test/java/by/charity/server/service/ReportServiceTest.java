package by.charity.server.service;

import by.charity.server.repository.DonationRepository;
import by.charity.server.repository.FundRepository;
import by.charity.server.repository.ReportRepository;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты ReportService")
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private FundRepository fundRepository;

    @Mock
    private UserService userService;

    private ReportService reportService;

    private User accountantUser;
    private User guestUser;
    private CharityFund testFund;

    @BeforeEach
    void setUp() {
        reportService = new ReportService();
        try {
            setField("reportRepository", reportRepository);
            setField("donationRepository", donationRepository);
            setField("fundRepository", fundRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        accountantUser = new User();
        accountantUser.setId(1L);
        accountantUser.setRole(Role.ACCOUNTANT);
        accountantUser.setActive(true);

        guestUser = new User();
        guestUser.setId(2L);
        guestUser.setRole(Role.GUEST);
        guestUser.setActive(true);

        testFund = new CharityFund();
        testFund.setId(1L);
        testFund.setName("Тестовый фонд");
        testFund.setTotalReceived(new BigDecimal("10000.00"));
        testFund.setTotalSpent(new BigDecimal("3000.00"));
        testFund.setActive(true);
    }

    private void setField(String name, Object value) throws Exception {
        var field = ReportService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(reportService, value);
    }

    @Test
    @DisplayName("Гость видит только публичные отчёты")
    void getAllReports_guestUser_returnsOnlyPublic() {
        when(userService.getUserByToken("guesttoken")).thenReturn(guestUser);

        Report publicReport = new Report();
        publicReport.setId(1L);
        publicReport.setPublic(true);
        publicReport.setTitle("Публичный отчёт");

        when(reportRepository.findAllPublic()).thenReturn(List.of(publicReport));

        List<Report> result = reportService.getAllReports("guesttoken", userService);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isPublic());
        verify(reportRepository, times(1)).findAllPublic();
        verify(reportRepository, never()).findAll();
    }

    @Test
    @DisplayName("Бухгалтер видит все отчёты")
    void getAllReports_accountant_returnsAll() {
        when(userService.getUserByToken("token")).thenReturn(accountantUser);

        Report r1 = new Report();
        r1.setPublic(true);
        Report r2 = new Report();
        r2.setPublic(false);
        when(reportRepository.findAll()).thenReturn(List.of(r1, r2));

        List<Report> result = reportService.getAllReports("token", userService);

        assertEquals(2, result.size());
        verify(reportRepository, times(1)).findAll();
        verify(reportRepository, never()).findAllPublic();
    }

    @Test
    @DisplayName("Генерация отчёта агрегирует пожертвования за период")
    void generateReport_validPeriod_correctAggregation() {
        doNothing().when(userService).requireRole(
                "token", Role.ADMIN, Role.MANAGER, Role.ACCOUNTANT);
        when(userService.getUserByToken("token")).thenReturn(accountantUser);
        when(fundRepository.findById(1L)).thenReturn(Optional.of(testFund));

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        Donation d1 = new Donation();
        d1.setAmount(new BigDecimal("500.00"));
        d1.setFundId(1L);
        d1.setDonatedAt(LocalDateTime.of(2026, 1, 15, 10, 0));

        Donation d2 = new Donation();
        d2.setAmount(new BigDecimal("300.00"));
        d2.setFundId(1L);
        d2.setDonatedAt(LocalDateTime.of(2026, 1, 20, 14, 0));

        // Пожертвование вне периода — не должно попасть в отчёт
        Donation d3 = new Donation();
        d3.setAmount(new BigDecimal("1000.00"));
        d3.setFundId(1L);
        d3.setDonatedAt(LocalDateTime.of(2025, 12, 31, 23, 0));

        when(donationRepository.findByFundId(1L))
                .thenReturn(List.of(d1, d2, d3));
        when(reportRepository.save(any())).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        Report result = reportService.generateReport(
                "token", 1L, null,
                Report.ReportType.MONTHLY,
                start, end, "Тест", false, userService);

        assertNotNull(result.getId());
        assertEquals(new BigDecimal("800.00"), result.getTotalReceived());
        assertEquals(2, result.getDonationsCount());
    }

    @Test
    @DisplayName("Генерация отчёта без прав доступа запрещена")
    void generateReport_insufficientRole_throwsAuthException() {
        doThrow(new CharityException.AuthException("Недостаточно прав"))
                .when(userService).requireRole(
                        "token", Role.ADMIN, Role.MANAGER, Role.ACCOUNTANT);

        assertThrows(CharityException.AuthException.class,
                () -> reportService.generateReport(
                        "token", 1L, null,
                        Report.ReportType.MONTHLY,
                        LocalDate.now(), LocalDate.now(),
                        "", false, userService));
    }


}